package server.rtsp.rtp;


import connect.network.udp.UdpClientTask;
import connect.network.udp.UdpFactory;
import connect.network.udp.UdpSender;
import server.rtsp.packet.RtpPacket;
import server.rtsp.rtcp.RtcpReportSocket;

/**
 * RtpSocket 传输rtp协议基于udp通信的类
 * Created by dell on 9/8/2017.
 *
 * @author yyz
 */
public class RtpSocket extends UdpClientTask {

    private RtcpReportSocket reportSocket;
    private boolean isPause = true;


    /**
     * rtp通讯
     *
     * @param address  目标地址
     * @param rtpPort  音视频数据端口
     * @param rtcpPort rtcp 端口
     */
    public RtpSocket(String address, int rtpPort, int rtcpPort) {
        setAddress(address, rtpPort);
        setSender(new UdpSender());
        reportSocket = new RtcpReportSocket(address, rtcpPort);
    }


    public int[] getLocalPorts() {
        return new int[]{
                getLocalPort(),
                reportSocket.getLocalPort()
        };
    }

    public void setPause(boolean pause) {
        isPause = pause;
    }

    public void setDestination(int rtpPort, int rtcpPort) {
        setAddress(getHost(), rtpPort);
        reportSocket.setAddress(reportSocket.getHost(), rtcpPort);
    }

    public void sendRtpPacket(RtpPacket packet) {
        if (!isPause) {
            UdpSender sender = getSender();
            //发送rtcp数据
            //reportSocket.update(packet.getLimit(), (packet.getTime() / 100L) * (packet.getClockFrequency() / 1000L) / 10000L);
            //reportSocket.update(packet.getLimit(), (packet.getTime() + packet.getClockFrequency() / 25) / 10000L);
            //发送rtp数据
            //rtpSocket.putSendData(packet.getData(), packet.getLimit());//4-8
            sender.sendData(packet.getData(), packet.getLimit());
        }
    }


    public void startConnect() {
        UdpFactory.getFactory().open();
        UdpFactory.getFactory().addTask(this);
        reportSocket.startConnect();
    }

    public void stopConnect() {
        UdpFactory.getFactory().removeTask(this);
        reportSocket.stopConnect();
    }


    public int getLocalPort() {
        if (getSocket() == null) {
            return 0;
        }
        return getSocket().getLocalPort();
    }

}
