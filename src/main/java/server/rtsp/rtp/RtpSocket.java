package server.rtsp.rtp;


import connect.network.udp.JavUdpConnect;
import server.rtsp.packet.NalPacket;
import server.rtsp.packet.RtpPacket;
import server.rtsp.rtcp.RtcpReportSocket;

import java.util.Random;

/**
 * RtpSocket 传输rtp协议基于udp通信的类
 * Created by dell on 9/8/2017.
 *
 * @author yyz
 */
public class RtpSocket {
    private RtcpReportSocket reportSocket;
    private JavUdpConnect rtpSocket;
    private RtpPacket rtpPacket;
    private int ssrc = new Random().nextInt();
    private int seq = 0;

    /**
     * rtp通讯
     *
     * @param address  目标地址
     * @param rtpPort  音视频数据端口
     * @param rtcpPort rtcp 端口
     */
    public RtpSocket(String address, int rtpPort, int rtcpPort) {
        rtpSocket = new JavUdpConnect(address, rtpPort);
        rtpSocket.setShutdownReceive(true);
//        ssrc = 54321;
        reportSocket = new RtcpReportSocket(address, rtcpPort, ssrc);
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
                rtpSocket.getLocalPort(),
//                12345
                reportSocket.getLocalPort()
        };
    }

    public void setDestination(String ip, int port, int rtcpPort) {
        rtpSocket.refreshDestAddress(ip, port);
        reportSocket.setDestination(ip, rtcpPort);
    }

    public void setDestination(int port, int rtcpPort) {
        rtpSocket.refreshDestAddress(port);
        reportSocket.setDestination(rtcpPort);
    }

    public RtpPacket sendNalPacket(NalPacket packet) {
        return sendNalPacket(packet, ++seq);
    }

    public RtpPacket sendNalPacket(NalPacket packet, int seq) {
        rtpPacket.setNalPacket(packet);//0-1 4-8
//        rtpPacket.markNextPacket();//1
        rtpPacket.setSSRC(ssrc);//8-12
        rtpPacket.setSeq(seq);//2-4
        sendRtpPacket(rtpPacket);
        return rtpPacket;
    }

    public void sendRtpPacket(RtpPacket packet) {
//        reportSocket.update(packet.getLimit(), (packet.getTime() / 100L) * (packet.getClockFrequency() / 1000L) / 10000L);
//        reportSocket.update(packet.getLimit(), (packet.getTime() + packet.getClockFrequency() / 25) / 10000L);
        rtpSocket.putSendData(packet.getData(), packet.getLimit());//4-8
    }


    public void startConnect() {
        rtpSocket.startConnect();
        reportSocket.startConnect();
    }

    public void stopConnect() {
        rtpSocket.stopConnect();
        reportSocket.stopConnect();
    }

    public void pauseConnect() {
        rtpSocket.pauseConnect();
    }

    public boolean isPauseConnect() {
        return rtpSocket.isPauseConnect();
    }

    public void continueConnect(boolean isCleanCache) {
        if (rtpSocket.isPauseConnect()) {
            rtpSocket.continueConnect(isCleanCache);
        }
    }

}
