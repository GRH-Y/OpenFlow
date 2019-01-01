package server.avedcoder.video;


import server.avedcoder.mp4.Mp4Analysis;
import server.rtsp.packet.ContentPacket;
import server.rtsp.packet.ParameterSetPacket;
import server.rtsp.packet.RtpPacket;
import task.executor.joggle.IConsumerAttribute;
import util.IoUtils;
import util.LogDog;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class DecodeMP4Stream extends VideoSteam {

    private String path;
    private RandomAccessFile file = null;
    private int[] arrayStco = null;
    private List<Mp4Analysis.Stsc> stscList = null;
    private volatile ParameterSetPacket parameterSetPacket = null;

    private long oldTime = 0, duration = 0, ts = 0;
    private byte[] header = new byte[5];
    private int type, maxLength = RtpPacket.MTU - RtpPacket.RTP_HEADER_LENGTH - 2;

    public DecodeMP4Stream(String path) {
        this.path = path;
    }


    private void initFile() throws Exception {
        Mp4Analysis mp4Analysis = new Mp4Analysis();
        mp4Analysis.init(path);
        mp4Analysis.findVideoBox();
        arrayStco = mp4Analysis.getStco();
        pps = mp4Analysis.getPPS();
        sps = mp4Analysis.getSPS();
        stscList = mp4Analysis.getListStsc();
        mp4Analysis.release();
        file = new RandomAccessFile(path, "r");
    }


    private void getFrame() {
        int fameIndex = 0;
        int stcoIndex = 0;
        try {
            Mp4Analysis.Stsc stsc = stscList.get(fameIndex);
            Mp4Analysis.Stsc nextStsc = stscList.get(1);
            do {
                if (stcoIndex + 1 == nextStsc.firstChunk) {
                    fameIndex++;
                    stsc = stscList.get(fameIndex);
                }
                file.seek(0);//恢复文件指针到0位置
                int stco = arrayStco[stcoIndex];
                IoUtils.skip(file, stco);//定位到要读取的位置

                for (int index = 0; index < stsc.SamplesPerChunk; index++) {
                    if (!extractData()) {
                        return;
                    }
                }
                LogDog.i("======================================================");
                stcoIndex++;
            }
            while (stcoIndex < arrayStco.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        taskContainer.getTaskExecutor().idleStopTask();
    }


    @Override
    protected boolean onBeginInit() {
        LogDog.w("==>DecodeMP4Stream startTask ing ....");
        try {
            initFile();
            parameterSetPacket = new ParameterSetPacket(sps, pps, RtpPacket.RTP_HEADER_LENGTH);
            parameterSetPacket.setFullNal(true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onEndInit() {
        if (!steamTrigger) {
            LogDog.d("==>DecodeMP4Stream 等待 RTSP播放指令!");
            taskContainer.getTaskExecutor().waitTask(0);
            LogDog.d("==>DecodeMP4Stream 收到 RTSP播放指令!");
        }
    }

    @Override
    protected void onCreateNalData() {
        getFrame();
    }

    /**
     * 提取数据
     */
    private boolean extractData() {
        IConsumerAttribute attribute = taskContainer.getAttribute();
        oldTime = System.nanoTime();
        IoUtils.readToFull(file, header);
        taskContainer.getTaskExecutor().sleepTask(35);
        ts += duration;
        int nalLength = Mp4Analysis.byteToInt(header, 0);
        type = header[4] & 0x1F;
        LogDog.v("==>DecodeMP4Stream type = " + type + " length = " + (nalLength + 4));


        //关键帧
        if (type == 5) {
            //发送sps和pps
            parameterSetPacket.setTime(ts);
            if (steamTrigger) {
                attribute.pushToCache(parameterSetPacket);
                resumeSteam();
            }
        }

        //type = 1(非关键帧)
        if (nalLength <= maxLength) {
            ContentPacket contentPacket = contentPacketCache.getRepeatData();
            if (contentPacket == null) {
                return false;
            }
            contentPacket.setTime(ts);
            byte[] data = contentPacket.getData();
            // Small NAL unit => Single NAL unit
            data[RtpPacket.RTP_HEADER_LENGTH] = header[4];
            nalLength--;
            int ret = IoUtils.readToFull(file, data, RtpPacket.RTP_HEADER_LENGTH + 1, nalLength);
            contentPacket.setFullNal(true);
            if (ret == IoUtils.FAIL) {
                stopSteam();
                return false;
            }
            contentPacket.setLimit(nalLength + RtpPacket.RTP_HEADER_LENGTH);
            if (steamTrigger) {
                attribute.pushToCache(contentPacket);
                resumeSteam();
                contentPacketCache.setRepeatData(contentPacket);
            }
        } else {
            // Large NAL unit => Split nal unit
            // Set FU-A header
            header[1] = (byte) (header[4] & 0x1F);  // FU header type
            header[1] += 0x80; // Start bit
            // Set FU-A indicator
            header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
            header[0] += 28;

            int sum = 1;
            while (sum < nalLength && taskContainer.getTaskExecutor().getLoopState()) {
                ContentPacket contentPacket = contentPacketCache.getRepeatData();
                if (contentPacket == null) {
                    return false;
                }
                byte[] data = contentPacket.getData();

                data[RtpPacket.RTP_HEADER_LENGTH] = header[0];
                data[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];

                int len = nalLength - sum > maxLength ? maxLength : nalLength - sum;
                int ret = IoUtils.readToFull(file, data, RtpPacket.RTP_HEADER_LENGTH + 2, len);
                if (ret == IoUtils.FAIL) {
                    stopSteam();
                    return false;
                }
                sum += len;
                // Last parameterSetPacket before next NAL
                if (sum >= nalLength) {
                    // End bit on
                    contentPacket.setFullNal(true);
                    data[RtpPacket.RTP_HEADER_LENGTH + 1] += 0x40;
                }
                contentPacket.setLimit(RtpPacket.RTP_HEADER_LENGTH + 2 + len);
                contentPacket.setTime(ts);
                if (steamTrigger) {
                    attribute.pushToCache(contentPacket);
                    resumeSteam();
                    contentPacketCache.setRepeatData(contentPacket);
                }
                // Switch startTask bit
                header[1] = (byte) (header[1] & 0x7F);
            }
        }
        duration = System.nanoTime() - oldTime;
        return true;
    }


    @Override
    protected void onDestroy() {
        LogDog.w("==>DecodeMP4Stream stopTask ing ....");
        try {
            file.close();
            file = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
