package server.avdecode.video;


import log.LogDog;
import server.camera.H264Preview;
import server.mp4.Mp4Analysis;
import server.mp4.NalAnalysis;
import server.rtsp.packet.RtpPacket;

/**
 * 编码线程（把h264封装成RTP包）
 * Created by dell on 9/7/2017.
 */

public class DVRH264Stream extends VideoSteam {
    private int width = 640;
    private int height = 480;
    private int pixelFormat = H264Preview.CAMERA_PIX_FMT_H264;
    private long oldTime = 0, duration = 0, ts = 0;
    private byte[] keyNal = null;
    private byte[] currentNal = null;


    public void setVideoSize(int width, int height, int pixelFormat) {
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
    }

    public byte[] getKeyNal() {
        return keyNal;
    }

    public String getBase64SPS() {
//        return "QgAo9AWh6IA=";
        while (sps == null && taskContainer.getTaskExecutor().isStartState()) {
            taskContainer.getTaskExecutor().waitTask(0);
        }
        return Mp4Analysis.getBase64SPS(sps);
    }

    public String getBase64PPS() {
//        return "zjiA";
        while (pps == null && taskContainer.getTaskExecutor().isStartState()) {
            taskContainer.getTaskExecutor().waitTask(0);
        }
        return Mp4Analysis.getBase64PPS(pps);
    }

    public String getProfileLevel() {
//        return "0028f4";
        while (sps == null && taskContainer.getTaskExecutor().isStartState()) {
            taskContainer.getTaskExecutor().waitTask(0);
        }
        return Mp4Analysis.getProfileLevel(sps);
    }

    @Override
    protected boolean onSteamBeginInit() {
        LogDog.w("--> DVR H264Stream start ing ....");
        //初始化摄像头
        return H264Preview.openCamera(width, height, pixelFormat);
    }

    @Override
    protected void onSteamEndInit() {
        if (!steamTrigger) {
            LogDog.d("==>DecodeMP4H264Stream 等待 RTSP播放指令!");
            taskContainer.getTaskExecutor().waitTask(0);
            LogDog.d("==>DecodeMP4H264Stream 收到 RTSP播放指令!");
        }
    }

    @Override
    protected void onSteamCreateNalData() {
//        IConsumerAttribute<NalPacket> attribute = taskContainer.getAttribute();
        oldTime = System.nanoTime();
        currentNal = H264Preview.getH264();
        if (currentNal == null) {
            stopSteam();
            return;
        }

//        int skipLength;
        ts += duration;

        NalAnalysis.NalInfo nalInfo = NalAnalysis.analysis(currentNal);
        if (nalInfo == null) {
            return;
        }
        if (nalInfo.isHasPPSAndSPS()) {
            sps = nalInfo.getSPS();
            pps = nalInfo.getPPS();
            keyNal = currentNal;
        }

        System.arraycopy(currentNal, 0, header, 0, header.length);
//        skipLength = 5;

        int type = header[4] & 0x1F;
        execNalData(header, type, currentNal.length, ts);

//        if (currentNal.length <= nalMaxLength) {
//            OtherFramePacket contentPacket = otherFramePacketCache.getRepeatData();
//            if (contentPacket == null) {
//                return;
//            }
//            contentPacket.setTime(ts);
//            byte[] framePacketData = contentPacket.getData();
//            framePacketData[RtpPacket.RTP_HEADER_LENGTH] = header[4];
//            skipLength--;
//            System.arraycopy(currentNal, skipLength, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 1, currentNal.length - skipLength);
//            LogDog.i("==> single rtp = " + byteToHexStr(framePacketData));
//            contentPacket.setFullNal(true);
//            contentPacket.setLimit(currentNal.length - skipLength + RtpPacket.RTP_HEADER_LENGTH);
//            if (steamTrigger) {
//                attribute.pushToCache(contentPacket);
//                resumeSteam();
//            }
//            otherFramePacketCache.setRepeatData(contentPacket);
//        } else {
//            header[1] = (byte) (header[4] & 0x1F);  // FU header type
//            header[1] |= 0x80; // Start bit
//            header[0] = (byte) (header[4] & 0x60); // FU indicator NRI
//            header[0] |= 28;
//
//            while (skipLength < currentNal.length) {
//                OtherFramePacket contentPacket = otherFramePacketCache.getRepeatData();
//                if (contentPacket == null) {
//                    contentPacket = new OtherFramePacket(RtpPacket.MTU);
//                }
//                byte[] data = contentPacket.getData();
//                contentPacket.setFullNal(false);
//
//                data[RtpPacket.RTP_HEADER_LENGTH] = header[0];
//                data[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];
//
//                int len = currentNal.length - skipLength > nalMaxLength ? nalMaxLength : currentNal.length - skipLength;
//                System.arraycopy(currentNal, skipLength, data, RtpPacket.RTP_HEADER_LENGTH + 2, len);
////                    LogDog.i("==> long rtp = " + Utils.bytes2HexString(data));
//                skipLength += len;
//                // Last keyFramePacket before next NAL
//                if (currentNal.length == skipLength) {
//                    // End bit on
//                    contentPacket.setFullNal(true);
//                    data[RtpPacket.RTP_HEADER_LENGTH + 1] |= 0x40;
//                }
//                contentPacket.setLimit(RtpPacket.RTP_HEADER_LENGTH + 2 + len);
//                contentPacket.setTime(ts);
//                if (steamTrigger) {
//                    attribute.pushToCache(contentPacket);
//                    resumeSteam();
//                }
//                otherFramePacketCache.setRepeatData(contentPacket);
//                // Switch start bit
//                header[1] = (byte) (header[1] & 0x7F);
//            }
//        }
        duration = System.nanoTime() - oldTime;
    }


    @Override
    protected void onSingleNal(byte[] framePacketData, int nalLength) {
//        System.arraycopy(currentNal, 5, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 1, currentNal.length - skipLength);
        System.arraycopy(currentNal, 5, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 1, currentNal.length - 5);
    }

    @Override
    protected int onSplitNal(byte[] framePacketData, int nalLength, int sum) {
        int len = nalLength - sum > nalMaxLength ? nalMaxLength : nalLength - sum;
        System.arraycopy(currentNal, sum, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 2, len);
        return len;
    }

    @Override
    protected void onSteamDestroy() {
        LogDog.w("--> DVR H264Stream  stop ing ....");
        H264Preview.stopCamera();
    }


//    public List<NalPacket> getKeyNalToNalPacket() {
//        RecorderTask recorderTask = container.getTask();
//        return recorderTask.packetNal(keyNal);
//    }


    //        private List<NalPacket> packetNal(byte[] nal) {
//            if (nal == null) {
//                return null;
//            }
//            List<NalPacket> result = new ArrayList<>();
//            byte[] header = new byte[5];
//            long ts = 0;
//            int skipLength;
//
//            System.arraycopy(nal, 0, header, 0, header.length);
//            skipLength = 5;
//
//
//            if (nal.length <= maxLength) {
//                OtherFramePacket contentPacket = new OtherFramePacket(RtpPacket.MTU);
//                contentPacket.setTime(ts);
//                byte[] data = contentPacket.getData();
//                data[RtpPacket.RTP_HEADER_LENGTH] = header[4];
//                skipLength--;
//                System.arraycopy(nal, skipLength, data, RtpPacket.RTP_HEADER_LENGTH + 1, nal.length - skipLength);
//                LogDog.i("==> single rtp = " + byteToHexStr(data));
//                contentPacket.setFullNal(true);
//                contentPacket.setLimit(nal.length - skipLength + RtpPacket.RTP_HEADER_LENGTH);
//                if (trigger) {
//                    IConsumerAttribute<NalPacket> executor = container.getAttribute();
//                    executor.pushToCache(contentPacket);
//                    resumeTask();
//                }
//            } else {
//                header[1] = (byte) (header[4] & 0x1F);  // FU header type
//                header[1] |= 0x80; // Start bit
//                header[0] = (byte) (header[4] & 0x60); // FU indicator NRI
//                header[0] |= 28;
//
//                while (skipLength < nal.length) {
//                    OtherFramePacket contentPacket = new OtherFramePacket(RtpPacket.MTU);
//                    byte[] data = contentPacket.getData();
//                    contentPacket.setFullNal(false);
//
//                    data[RtpPacket.RTP_HEADER_LENGTH] = header[0];
//                    data[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];
//
//                    int len = nal.length - skipLength > maxLength ? maxLength : nal.length - skipLength;
//                    System.arraycopy(nal, skipLength, data, RtpPacket.RTP_HEADER_LENGTH + 2, len);
////                    LogDog.i("==> key rtp = " + Utils.bytes2HexString(data));
//                    skipLength += len;
//                    // Last keyFramePacket before next NAL
//                    if (nal.length == skipLength) {
//                        // End bit on
//                        contentPacket.setFullNal(true);
//                        data[RtpPacket.RTP_HEADER_LENGTH + 1] |= 0x40;
//                    }
//                    contentPacket.setLimit(RtpPacket.RTP_HEADER_LENGTH + 2 + len);
//                    contentPacket.setTime(ts);
//                    result.add(contentPacket);
//                    // Switch start bit
//                    header[1] = (byte) (header[1] & 0x7F);
//                }
//            }
//            return result;
//        }


    private String byteToHexStr(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < b.length; n++) {
            String tmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((tmp.length() == 1) ? "0" + tmp : tmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

}
