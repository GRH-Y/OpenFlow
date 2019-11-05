package server.avdecode.video;

import server.avdecode.NalSteam;
import server.rtsp.packet.KeyFramePacket;
import server.rtsp.packet.NalPacket;
import server.rtsp.packet.OtherFramePacket;
import server.rtsp.packet.RtpPacket;
import task.executor.joggle.IConsumerAttribute;
import util.IoEnvoy;
import util.MultiplexCache;

import java.util.Base64;

/**
 * 视频流
 */
public abstract class VideoSteam extends NalSteam {

    protected KeyFramePacket keyFramePacket = null;
    protected MultiplexCache<OtherFramePacket> otherFramePacketCache = null;

    protected byte[] sps = null;
    protected byte[] pps = null;

    protected byte[] header = new byte[5];
    protected int nalMaxLength = RtpPacket.MTU - RtpPacket.RTP_HEADER_LENGTH - 2;


    public String getBase64SPS() {
//        return "Z0KAH9oBQBboBtChNQ==";
        return Base64.getEncoder().encodeToString(sps);
    }

    public String getBase64PPS() {
//        return "aM4G4g==";
        return Base64.getEncoder().encodeToString(pps);
    }

    public String getProfileLevel() {
//        return "42801f";
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


    private void initCache() {
        keyFramePacket = new KeyFramePacket(sps, pps, RtpPacket.RTP_HEADER_LENGTH);
        keyFramePacket.setFullNal(true);

        int size = 100;
        otherFramePacketCache = new MultiplexCache<>(size);
        for (int i = 0; i < size; i++) {
            OtherFramePacket packet = new OtherFramePacket(RtpPacket.MTU);
            otherFramePacketCache.setRepeatData(packet);
        }
    }

    @Override
    protected void onSteamEndInit() {
        initCache();
    }

    @Override
    protected void onSteamDestroy() {
        if (otherFramePacketCache != null) {
            otherFramePacketCache.release();
            otherFramePacketCache = null;
        }
    }

    /**
     * 回调封装好的nal
     *
     * @param nalPacket
     */
    protected void onFrameNalPacket(NalPacket nalPacket) {
        if (steamTrigger) {
            IConsumerAttribute attribute = taskContainer.getTaskExecutor().getAttribute();
            attribute.pushToCache(nalPacket);
            resumeSteam();
        }
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
    protected abstract int onSplitNal(byte[] framePacketData, int nalLength, int sum);


    /**
     * 封装nal
     *
     * @param type
     * @param nalLength
     */
    protected void execNalData(byte[] header, int type, int nalLength, long ts) {
        //关键帧
        if (type == 5) {
            //发送sps和pps
            keyFramePacket.setTime(ts);
            onFrameNalPacket(keyFramePacket);
        }

        //type = 1(非关键帧)
        if (nalLength <= nalMaxLength) {
            OtherFramePacket otherFramePacket = otherFramePacketCache.getRepeatData();
            if (otherFramePacket != null) {
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
                onFrameNalPacket(otherFramePacket);
                otherFramePacketCache.setRepeatData(otherFramePacket);
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
                OtherFramePacket otherFramePacket = otherFramePacketCache.getRepeatData();
                if (otherFramePacket != null) {
                    byte[] framePacketData = otherFramePacket.getData();
                    framePacketData[RtpPacket.RTP_HEADER_LENGTH] = header[0];
                    framePacketData[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];
                    int len = onSplitNal(framePacketData, nalLength, sum);
                    if (len != IoEnvoy.FAIL) {
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
                        onFrameNalPacket(otherFramePacket);
                        otherFramePacketCache.setRepeatData(otherFramePacket);
                        // Switch startTask bit
                        header[1] = (byte) (header[1] & 0x7F);
                    }
                }
            }
        }
    }

}
