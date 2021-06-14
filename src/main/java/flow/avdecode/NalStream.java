package flow.avdecode;

import flow.core.FlowStream;
import flow.core.joggle.SteamType;
import flow.rtsp.packet.NalPacket;
import flow.rtsp.packet.RtcpPacket;

import java.util.Random;

/**
 * nal流
 */
public abstract class NalStream extends FlowStream<NalPacket> {


    private int ssrc = new Random().nextInt();
    private RtcpPacket rtcpPacket;

    /**
     * 创建NalSteam
     */
    public NalStream(SteamType type) {
        super(type);
        rtcpPacket = new RtcpPacket(ssrc);
    }


    public int getSSRC() {
        return ssrc;
    }

    public RtcpPacket getRtcpPacket() {
        return rtcpPacket;
    }

    /**
     * 输出 nal 数据
     *
     * @param packet
     */
    protected void pushNalPacket(NalPacket packet) {
        if (pushListener != null) {
            pushListener.onStreamDataPush(packet);
        }
    }

}
