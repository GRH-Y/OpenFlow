package server.avdecode.video;


import server.avdecode.mp4.Mp4Analysis;
import server.avdecode.mp4.Mp4Box;
import server.rtsp.packet.RtpPacket;
import util.IoUtils;
import util.LogDog;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class DecodeMP4H264Stream extends VideoSteam {

    private String path;
    private int[] arrayStco = null;
    private RandomAccessFile file = null;
    private List<Mp4Analysis.Stsc> stscList = null;

//    private List<Mp4Box.StscBox.Stsc> stscList = null;


    private long oldTime = 0, duration = 0, ts = 0;

    private int type;

    public DecodeMP4H264Stream(String path) {
        this.path = path;
    }


    private void initFile() throws Exception {
//        Mp4Box mp4Box = new Mp4Box(path);
//        try {
//            mp4Box.analysis();
//            Mp4Box.MoovBox moovBox = mp4Box.getMoovBox();
//            Mp4Box.StcoBox stcoBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stcoBox;
//            arrayStco = stcoBox.stcoArray;
//            Mp4Box.StscBox stscBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stscBox;
//            stscList = stscBox.stscList;
//            //sps pps
//            Mp4Box.StsdBox stsdBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stsdBox;
//            pps = stsdBox.pps;
//            sps = stsdBox.sps;
////            mp4Box.release();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Mp4Analysis mp4Analysis = new Mp4Analysis();
        mp4Analysis.init(path);
        mp4Analysis.findVideoBox();
        pps = mp4Analysis.getPPS();
        sps = mp4Analysis.getSPS();
        arrayStco = mp4Analysis.getStco();
        stscList = mp4Analysis.getListStsc();
        mp4Analysis.release();
        file = new RandomAccessFile(path, "r");
    }


    @Override
    protected boolean onSteamBeginInit() {
        LogDog.w("==>DecodeMP4H264Stream startTask ing ....");
        try {
            initFile();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onSteamEndInit() {
        super.onSteamEndInit();
        if (!steamTrigger) {
            LogDog.d("==>DecodeMP4H264Stream 等待 RTSP播放指令!");
            taskContainer.getTaskExecutor().waitTask(0);
            LogDog.d("==>DecodeMP4H264Stream 收到 RTSP播放指令!");
        }
    }

    @Override
    protected void onSteamCreateNalData() {
        int fameIndex = 0;
        int stcoIndex = 0;
        try {
            Mp4Analysis.Stsc stsc = stscList.get(fameIndex);
            Mp4Analysis.Stsc nextStsc = stscList.get(1);
//            Mp4Box.StscBox.Stsc stsc = stscList.get(fameIndex);
//            Mp4Box.StscBox.Stsc nextStsc = stscList.get(1);
            do {
                if (stcoIndex + 1 == nextStsc.firstChunk) {
                    fameIndex++;
                    stsc = stscList.get(fameIndex);
                }
                file.seek(0);//恢复文件指针到0位置
                int stco = arrayStco[stcoIndex];
                IoUtils.skip(file, stco);//定位到要读取的位置


                for (int index = 0; index < stsc.SamplesPerChunk; index++) {
                    if (!taskContainer.getTaskExecutor().getLoopState()) {
                        return;
                    }
                    oldTime = System.nanoTime();
                    int len = IoUtils.readToFull(file, header);
                    if (len == IoUtils.FAIL) {
                        return;
                    }
                    taskContainer.getTaskExecutor().sleepTask(33);
                    ts += duration;
                    int nalLength = Mp4Analysis.byteToInt(header, 0);
                    type = header[4] & 0x1F;
                    LogDog.v("==>DecodeMP4H264Stream type = " + type + " length = " + nalLength);
                    execNalData(header, type, nalLength, ts);
                    duration = System.nanoTime() - oldTime;
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


    /**
     * 单个nal回调
     *
     * @param framePacketData
     * @param nalLength
     */
    protected void onSingleNal(byte[] framePacketData, int nalLength) {
        int ret = IoUtils.readToFull(file, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 1, nalLength);
        if (ret == IoUtils.FAIL) {
            stopSteam();
        }
    }

    /**
     * 切分nal回调
     */
    protected int onSplitNal(byte[] framePacketData, int nalLength, int sum) {
        int len = nalLength - sum > nalMaxLength ? nalMaxLength : nalLength - sum;
        int ret = IoUtils.readToFull(file, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 2, len);
        if (ret == IoUtils.FAIL) {
            stopSteam();
        }
        return len;
    }


    @Override
    protected void onSteamDestroy() {
        super.onSteamDestroy();
        LogDog.w("==>DecodeMP4H264Stream stopTask ing ....");
        try {
            file.close();
            file = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
