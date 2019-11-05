package server.avdecode.video;


import log.LogDog;
import server.mp4.Mp4Box;
import server.rtsp.packet.RtpPacket;
import util.IoEnvoy;
import util.TypeConversion;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class DecodeMP4H264Stream extends VideoSteam {

    private String path;
    private RandomAccessFile file = null;

    private List<Mp4Box.StscBox.Stsc> stscList = null;
    private List<Mp4Box.StcoBox.Stco> stcoList = null;
    private List<Mp4Box.CttsBox.Ctts> cttsList = null;


    private long oldTime = 0, duration = 0, ts = 0;

    private int type;

    public DecodeMP4H264Stream(String path) {
        this.path = path;
    }


    private void initFile() throws Exception {
        Mp4Box mp4Box = new Mp4Box(path);
        mp4Box.analysis();
        Mp4Box.MoovBox moovBox = mp4Box.getMoovBox();
        Mp4Box.StcoBox stcoBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stcoBox;
        stcoList = stcoBox.stcoList;
        Mp4Box.StscBox stscBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stscBox;
        stscList = stscBox.stscList;
        Mp4Box.CttsBox cttsBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.cttsBox;
        cttsList = cttsBox.cttsList;
        //sps pps
        Mp4Box.StsdBox stsdBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stsdBox;
        pps = stsdBox.pps;
        sps = stsdBox.sps;
        mp4Box.release();
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

        int cttsIndex = -1;
        int cttsCount = 0;

        try {
            Mp4Box.StscBox.Stsc stsc = stscList.get(fameIndex);
            Mp4Box.StscBox.Stsc nextStsc = null;
            Mp4Box.CttsBox.Ctts ctts = null;
            if (stscList.size() > 1) {
                nextStsc = stscList.get(1);
            }

//            ts = System.currentTimeMillis();
            long test = 0;
            do {
                if (nextStsc != null && stcoIndex + 1 == nextStsc.firstChunk) {
                    fameIndex++;
                    stsc = stscList.get(fameIndex);
                }

                file.seek(0);//恢复文件指针到0位置
                int stco = stcoList.get(stcoIndex).sampleSize;
                IoEnvoy.skip(file, stco);//定位到要读取的位置

                for (int index = 0; index < stsc.SamplesPerChunk; index++) {
                    if (!taskContainer.getTaskExecutor().getLoopState()) {
                        return;
                    }

                    int len = IoEnvoy.readToFull(file, header);
                    if (len == IoEnvoy.FAIL) {
                        return;
                    }

//                    if (cttsCount == 0) {
//                        cttsIndex++;
//                        if (cttsIndex < cttsList.size()) {
//                            ctts = cttsList.get(cttsIndex);
//                            cttsCount = ctts.sampleCount;
//                        }
//                        if (ctts.sampleOffset > 0) {
//                            test += ctts.sampleOffset / 100;
//                        }
//                    }


                    long time = 33;
//                    if (ctts.sampleOffset == 0) {
//                        time = ctts.sampleOffset;
//                    } else
//                    if (ctts.sampleOffset > 0) {
//                        time = ctts.sampleOffset / 100;
//                    }

                    long now = System.currentTimeMillis();
                    taskContainer.getTaskExecutor().sleepTask(time);
                    if (oldTime == 0) {
                        ts = now;
                    } else if (oldTime > 0) {
                        duration = 3000 + now - oldTime;
//                        duration = 3000;
                        ts += duration;
//                        test = ts;
                    }
//                    if (ts == 0) {
//                        ts = oldTime;
//                    } else {
//                        ts = 10000;
//                    }
                    int nalLength = TypeConversion.byteToInt(header, 0);
                    type = header[4] & 0x1F;
                    LogDog.v("==>DecodeMP4H264Stream type = " + type + " length = " + nalLength + " ts = " + ts);
                    //上一次时间戳 + 采样频率（典型值为90000）*0.000001 *  两帧时间差（单位毫秒）
                    execNalData(header, type, nalLength, ts);
//                    duration = System.nanoTime() - oldTime;
                    oldTime = now;
//                    cttsCount--;
//                    duration += 3600;
                }
                LogDog.i("======================================================");
                stcoIndex++;
            }
            while (stcoIndex < stcoList.size());
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
        int ret = IoEnvoy.readToFull(file, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 1, nalLength);
        if (ret == IoEnvoy.FAIL) {
            stopSteam();
        }
    }

    /**
     * 切分nal回调
     */
    protected int onSplitNal(byte[] framePacketData, int nalLength, int sum) {
        int len = nalLength - sum > nalMaxLength ? nalMaxLength : nalLength - sum;
        int ret = IoEnvoy.readToFull(file, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 2, len);
        if (ret == IoEnvoy.FAIL) {
            stopSteam();
        }
        return len;
    }


    @Override
    protected void onSteamDestroy() {
        super.onSteamDestroy();
        LogDog.w("==>DecodeMP4H264Stream stopTask ing ....");
        if (file != null) {
            try {
                file.close();
                file = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
