package flow.avdecode.video;


import flow.camera.H264Preview;
import flow.mp4.NalAnalysis;
import flow.rtsp.packet.RtpPacket;
import log.LogDog;

/**
 * 编码线程（把h264封装成RTP包）
 * Created by dell on 9/7/2017.
 */

public class DVRH264Stream extends H264VideoStream {
    private int width = 640;
    private int height = 480;
    private int pixelFormat = H264Preview.CAMERA_PIX_FMT_H264;
    private long oldTime = 0, duration = 0, ts = 0;
    private byte[] keyNal = null;
    private byte[] otherNal = null;


    public void setVideoSize(int width, int height, int pixelFormat) {
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
    }

    public byte[] getKeyNal() {
        return keyNal;
    }

    @Override
    protected boolean onSteamBeginInit() {
        LogDog.w("--> DVR H264Stream start ing ....");
        //初始化摄像头
        boolean ret = H264Preview.openCamera(width, height, pixelFormat);
        if (ret) {
            pauseSteam();
        }
        return ret;
    }


    @Override
    protected void onSteamExeNalData(boolean initResult) {
        if (!initResult) {
            return;
        }
//        IConsumerAttribute<NalPacket> attribute = taskContainer.getAttribute();
        oldTime = System.nanoTime();
        otherNal = H264Preview.getH264();
        if (otherNal == null) {
            stopSteam();
            return;
        }

//        int skipLength;
        ts += duration;

        NalAnalysis.NalInfo nalInfo = NalAnalysis.analysis(otherNal);
        if (nalInfo == null) {
            return;
        }
        if (nalInfo.isHasPPSAndSPS()) {
            configSteam(nalInfo.getSPS(), nalInfo.getPPS());
            keyNal = otherNal;
        }

        System.arraycopy(otherNal, 0, header, 0, header.length);
//        skipLength = 5;

        int type = header[4] & 0x1F;
        packetNalData(header, type, otherNal.length, ts);

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
        System.arraycopy(otherNal, 5, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 1, otherNal.length - 5);
    }

    @Override
    protected int onSliceNal(byte[] framePacketData, int nalLength, int sum) {
        int len = nalLength - sum > nalMaxLength ? nalMaxLength : nalLength - sum;
        System.arraycopy(otherNal, sum, framePacketData, RtpPacket.RTP_HEADER_LENGTH + 2, len);
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
