package server.avdecode.video;

import server.avdecode.NalSteam;
import server.avdecode.mp4.Mp4Analysis;
import server.rtsp.packet.ContentPacket;
import server.rtsp.packet.ParameterSetPacket;
import server.rtsp.packet.RtpPacket;
import util.MultiplexCache;

/**
 * 视频流
 */
public abstract class VideoSteam extends NalSteam {

    protected ParameterSetPacket parameterSetPacket = null;
    protected MultiplexCache<ContentPacket> contentPacketCache = null;

    protected byte[] sps = null;
    protected byte[] pps = null;

    protected int nalMaxLength = RtpPacket.MTU - RtpPacket.RTP_HEADER_LENGTH - 2;


    public String getBase64SPS() {
//        return "Z0KAH9oBQBboBtChNQ==";
        return Mp4Analysis.getBase64SPS(sps);
    }

    public String getBase64PPS() {
//        return "aM4G4g==";
        return Mp4Analysis.getBase64PPS(pps);
    }

    public String getProfileLevel() {
//        return "42801f";
        return Mp4Analysis.getProfileLevel(sps);
    }


    private void initCache() {
        parameterSetPacket = new ParameterSetPacket(sps, pps, RtpPacket.RTP_HEADER_LENGTH);
        parameterSetPacket.setFullNal(true);

        int size = 100;
        contentPacketCache = new MultiplexCache<>(size);
        for (int i = 0; i < size; i++) {
            ContentPacket packet = new ContentPacket(RtpPacket.MTU);
            contentPacketCache.setRepeatData(packet);
        }
    }

    @Override
    protected void onSteamEndInit() {
        initCache();
    }

    @Override
    protected void onSteamDestroy() {
        if (contentPacketCache != null) {
            contentPacketCache.release();
            contentPacketCache = null;
        }
    }

}
