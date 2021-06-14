package flow.avdecode.video;


import flow.mp4.Mp4Box;
import flow.rtsp.packet.RtpPacket;
import log.LogDog;
import util.IoEnvoy;
import util.TypeConversion;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class DecodeMP4H264Stream extends H264VideoStream {

    private File mp4File;
    private RandomAccessFile file = null;

    private List<Mp4Box.StscBox.Stsc> stscList = null;
    private List<Mp4Box.StcoBox.Stco> stcoList = null;
    private List<Mp4Box.CttsBox.Ctts> cttsList = null;


    private long oldTime = 0, duration = 0, ts = 0;

    private int type;

    public DecodeMP4H264Stream(String path) {
        mp4File = new File(path);
        if (!mp4File.exists()) {
            throw new RuntimeException("mp4File is no found = " + path);
        }
    }


    private void initFile() throws Exception {
        Mp4Box mp4Box = new Mp4Box(mp4File);
        mp4Box.analysis();
        Mp4Box.MoovBox moovBox = mp4Box.getMoovBox();
        Mp4Box.StcoBox stcoBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stcoBox;
        stcoList = stcoBox.stcoList;
        Mp4Box.StscBox stscBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stscBox;
        stscList = stscBox.stscList;
        Mp4Box.CttsBox cttsBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.cttsBox;
        Mp4Box.SttsBox sttsBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.sttsBox;
        if (cttsBox != null) {
            cttsList = cttsBox.cttsList;
        }
        //sps pps
        Mp4Box.StsdBox stsdBox = moovBox.trakBoxes.get(0).mdiaBox.minfBox.stblBox.stsdBox;
        configSteam(stsdBox.sps, stsdBox.pps);
        mp4Box.release();
        file = new RandomAccessFile(mp4File, "r");
    }


    @Override
    protected boolean onSteamBeginInit() {
        LogDog.w("==>DecodeMP4H264Stream startTask ing ....");
        try {
            initFile();
            //暂停任务，等待触发
            pauseSteam();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onSteamExeNalData(boolean initResult) {
        if (!initResult) {
            return;
        }
        int fameIndex = 0;
        int stcoIndex = 0;

        int cttsIndex = 0;
        int cttsCount = 0;

        long offset = 0;
        long waitTime = 41;

        Mp4Box.StscBox.Stsc stsc = stscList.get(fameIndex);
        Mp4Box.StscBox.Stsc nextStsc = null;
        Mp4Box.CttsBox.Ctts ctts = cttsList.get(cttsIndex);
        if (stscList.size() > 1) {
            nextStsc = stscList.get(1);
        }

        if (ctts != null) {
            cttsCount = ctts.sampleCount;
            offset = ctts.sampleOffset;
            if (offset > 0) {
                waitTime = ctts.sampleOffset / 100;
            }
        }


        do {
            if (nextStsc != null && stcoIndex + 1 == nextStsc.firstChunk) {
                fameIndex++;
                stsc = stscList.get(fameIndex);
            }

            try {
                //恢复文件指针到0位置
                file.seek(0);
                int stco = stcoList.get(stcoIndex).sampleSize;
                //定位到要读取的位置
                file.skipBytes(stco);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            for (int index = 0; index < stsc.SamplesPerChunk; index++) {
                if (!taskContainer.getTaskExecutor().isLoopState()) {
                    return;
                }

                int len = IoEnvoy.readToFull(file, header);
                if (len == IoEnvoy.FAIL) {
                    return;
                }

                if (cttsCount == 0 && ctts != null) {
                    cttsIndex++;
                    if (cttsIndex < cttsList.size()) {
                        ctts = cttsList.get(cttsIndex);
                        cttsCount = ctts.sampleCount;
                        offset = ctts.sampleOffset;
                        if (offset > 0) {
                            waitTime = ctts.sampleOffset / 100;
                        } else {
                            offset = waitTime * 100;
                        }
//                    LogDog.d("==> waitTime = " + waitTime);
                    }
                }

                taskContainer.getTaskExecutor().sleepTask(waitTime);

                int nalLength = TypeConversion.byteToInt(header, 0);
                type = header[4] & 0x1F;
//                LogDog.v("==>DecodeMP4H264Stream type = " + type + " length = " + nalLength + " ts = " + ts);
                //上一次时间戳 + 采样频率（典型值为90000）*0.000001 *  两帧时间差（单位毫秒）
                packetNalData(header, type, nalLength, ts);
                ts += offset;

                cttsCount--;
            }
//            LogDog.i("======================================================");
            stcoIndex++;
        }
        while (stcoIndex < stcoList.size());
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
    protected int onSliceNal(byte[] framePacketData, int nalLength, int sum) {
        int len = nalLength - sum > nalMaxLength ? nalMaxLength : nalLength - sum;
        int ret = IoEnvoy.readToFull(file, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 2, len);
        if (ret == IoEnvoy.FAIL) {
            len = IoEnvoy.FAIL;
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
