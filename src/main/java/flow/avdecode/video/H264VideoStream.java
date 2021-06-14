package flow.avdecode.video;

import flow.avdecode.NalStream;
import flow.core.joggle.SteamType;
import flow.rtsp.packet.KeyFramePacket;
import flow.rtsp.packet.NalPacket;
import flow.rtsp.packet.OtherFramePacket;
import flow.rtsp.packet.RtpPacket;
import security.Base64Helper;
import util.MultiplexCache;

/**
 * 视频流
 */
public abstract class H264VideoStream extends NalStream {

    protected MultiplexCache<KeyFramePacket> packetKeyCache = null;
    protected MultiplexCache<OtherFramePacket> packetOtherCache = null;

    private byte[] sps = null;
    private byte[] pps = null;

    /**
     * 视频帧率
     */
    private long videoClock = 90000;

    protected byte[] header = new byte[5];
    protected int nalMaxLength = RtpPacket.MTU - RtpPacket.RTP_HEADER_LENGTH - 2;

    /**
     * 创建 VideoStream
     */
    public H264VideoStream() {
        super(SteamType.VIDEO);
    }


    public String getBase64SPS() {
        if (sps == null) {
            return null;
        }
//        return "Z0KAH9oBQBboBtChNQ==";
        return Base64Helper.getHelper().encodeToString(sps, Base64Helper.FLAGS.NO_WRAP);
    }

    public String getBase64PPS() {
        if (pps == null) {
            return null;
        }
//        return "aM4G4g==";
        return Base64Helper.getHelper().encodeToString(pps, Base64Helper.FLAGS.NO_WRAP);
    }

    public String getProfileLevel() {
        if (sps == null) {
            return null;
        }
//        return "42801f";
        return getHexString(sps);
    }


    public long getClockFrequency() {
        return videoClock;
    }

    protected void configSteam(byte[] sps, byte[] pps) {

        this.sps = sps;
        this.pps = pps;

        packetKeyCache = new MultiplexCache<>();
        packetKeyCache.addData(createKeyFramePacket());

        packetOtherCache = new MultiplexCache<>();
        packetOtherCache.addData(createOtherFramePacket());
    }

    private OtherFramePacket createOtherFramePacket() {
        return new OtherFramePacket(RtpPacket.MTU);
    }

    private KeyFramePacket createKeyFramePacket() {
        KeyFramePacket keyFramePacket = new KeyFramePacket(sps, pps, RtpPacket.RTP_HEADER_LENGTH);
        keyFramePacket.setFullNal(true);
        return keyFramePacket;
    }

    public void resetNalPacket(NalPacket packet) {
        if (packet instanceof OtherFramePacket) {
            packetOtherCache.resetData((OtherFramePacket) packet);
        } else if (packet instanceof KeyFramePacket) {
            packetKeyCache.resetData((KeyFramePacket) packet);
        }
    }

    private String getHexString(byte[] sps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < 4; i++) {
            String str = Integer.toHexString(sps[i] & 0xFF);
            if (str.length() < 2) {
                builder.append("0");
            }
            builder.append(str);
        }
        return builder.toString();
    }

    @Override
    protected void onSteamDestroy() {
        if (packetOtherCache != null) {
            packetOtherCache.release();
        }
        if (packetKeyCache != null) {
            packetKeyCache.release();
        }
        sps = null;
        pps = null;
    }

    /**
     * 单个nal回调
     *
     * @param framePacketData
     * @param nalLength
     */
    protected abstract void onSingleNal(byte[] framePacketData, int nalLength);

    /**
     * 切分nal回调
     */
    protected abstract int onSliceNal(byte[] framePacketData, int nalLength, int sum);


    /**
     * 封装nal
     *
     * @param type
     * @param nalLength
     */
    protected void packetNalData(byte[] header, int type, int nalLength, long ts) {
        //关键帧
        if (type == 5) {
            //发送sps和pps
            KeyFramePacket keyFramePacket = packetKeyCache.getCanUseData();
            if (keyFramePacket == null) {
                keyFramePacket = createKeyFramePacket();
            }
            keyFramePacket.setTime(ts);
            pushNalPacket(keyFramePacket);
        }

        //type = 1(非关键帧)
        if (nalLength <= nalMaxLength) {
            OtherFramePacket otherFramePacket = packetOtherCache.getCanUseData();
            if (otherFramePacket == null) {
                otherFramePacket = createOtherFramePacket();
            }
            otherFramePacket.setTime(ts);
            byte[] framePacketData = otherFramePacket.getData();
            // Small NAL unit => Single NAL unit
            framePacketData[RtpPacket.RTP_HEADER_LENGTH] = header[4];
            nalLength--;
            //执行回调单nal
            onSingleNal(framePacketData, nalLength);
            otherFramePacket.setFullNal(true);
            otherFramePacket.setLimit(nalLength + RtpPacket.RTP_HEADER_LENGTH);
            //回调封装好的nal包
            pushNalPacket(otherFramePacket);
        } else {
            // Large NAL unit => Split nal unit
            // Set FU-A header
            header[1] = (byte) (header[4] & 0x1F);  // FU header type
            header[1] += 0x80; // Start bit
            // Set FU-A indicator
            header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
            header[0] += 28;
            int sum = 1;

            while (sum < nalLength && taskContainer.getTaskExecutor().isLoopState()) {
                OtherFramePacket otherFramePacket = packetOtherCache.getCanUseData();
                if (otherFramePacket == null) {
                    otherFramePacket = createOtherFramePacket();
                }
                otherFramePacket.setFullNal(false);
                byte[] framePacketData = otherFramePacket.getData();
                framePacketData[RtpPacket.RTP_HEADER_LENGTH] = header[0];
                framePacketData[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];
                int len = onSliceNal(framePacketData, nalLength, sum);
                if (len <= 0) {
                    break;
                } else {
                    sum += len;
                    // Last keyFramePacket before next NAL
                    if (sum >= nalLength) {
                        // End bit on
                        otherFramePacket.setFullNal(true);
                        framePacketData[RtpPacket.RTP_HEADER_LENGTH + 1] += 0x40;
                    }
                    otherFramePacket.setTime(ts);
                    otherFramePacket.setLimit(RtpPacket.RTP_HEADER_LENGTH + 2 + len);
                    //回调封装好的nal包
                    pushNalPacket(otherFramePacket);
                    // Switch startTask bit
                    header[1] = (byte) (header[1] & 0x7F);
                }
            }
        }
    }

}
