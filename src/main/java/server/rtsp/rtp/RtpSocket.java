package server.rtsp.rtp;


import connect.network.udp.JavUdpConnect;
import server.avedcoder.packet.NalPacket;
import server.avedcoder.packet.RtpPacket;
import server.rtsp.rtcp.RtcpReportSocket;

import java.util.Random;

/**
 * RtpSocket 传输rtp协议基于udp通信的类
 * Created by dell on 9/8/2017.
 *
 * @author yyz
 */
public class RtpSocket {
    private RtcpReportSocket reportSocket = null;
    private JavUdpConnect connect = null;
    private RtpPacket rtpPacket = null;
    private int ssrc = 0;
    private int seq = 0;

    /**
     * rtp通讯
     *
     * @param ip       目标地址
     * @param port     音视频数据端口
     * @param rtcpPort rtcp 端口
     */
    public RtpSocket(String ip, int port, int rtcpPort) {
        connect = new JavUdpConnect(ip, port);
        connect.setMaxCache(0);
        connect.setShutdownReceive(true);
        ssrc = new Random().nextInt();
        ssrc = 54321;
        reportSocket = new RtcpReportSocket(ip, rtcpPort, ssrc);
        rtpPacket = new RtpPacket();
    }

    public int getSSRC() {
        return ssrc;
    }

    /**
     * Sets the clock frequency of the stream in Hz.
     */
    public void setClockFrequency(long clock) {
        rtpPacket.setClockFrequency(clock);
    }

    public int[] getLocalPorts() {
        return new int[]{
                connect.getLocalPort(),
//                12345
                reportSocket.getLocalPort()
        };
    }

    public void setDestination(String ip, int port, int rtcpPort) {
        connect.refreshDestAddress(ip, port);
        reportSocket.setDestination(ip, rtcpPort);
    }

    public void setDestination(int port, int rtcpPort) {
        connect.refreshDestAddress(port);
        reportSocket.setDestination(rtcpPort);
    }

    public RtpPacket sendNalPacket(NalPacket packet) {
        return sendNalPacket(packet, ++seq);
    }

    public RtpPacket sendNalPacket(NalPacket packet, int seq) {
        rtpPacket.setNalPacket(packet);//0,1
        rtpPacket.markNextPacket();//1
        rtpPacket.setSSRC(ssrc);//8-12
        rtpPacket.setSeq(seq);
        sendRtpPacket(rtpPacket);
        return rtpPacket;
    }

    public void sendRtpPacket(RtpPacket packet) {
//        reportSocket.update(packet.getLimit(), (packet.getTime() / 100L) * (packet.getClockFrequency() / 1000L) / 10000L);
        connect.putSendData(packet.getData(), packet.getLimit());//4-8
    }


    public void startConnect() {
        connect.startConnect();
        reportSocket.startConnect();
    }

    public void stopConnect() {
        connect.stopConnect();
        reportSocket.stopConnect();
    }

    public void pauseConnect() {
        connect.pauseConnect();
    }

    public boolean isPauseConnect() {
        return connect.isPauseConnect();
    }

    public void continueConnect(boolean isCleanCache) {
        if (connect.isPauseConnect()) {
            connect.continueConnect(isCleanCache);
        }
    }

}
