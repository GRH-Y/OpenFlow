package flow.rtsp.rtp;


import connect.network.base.NetTaskStatus;
import connect.network.udp.UdpServerTask;
import flow.rtsp.rtcp.RtcpReportSocket;

/**
 * RtpSocket 传输rtp协议基于udp通信的类
 * Created by dell on 9/8/2017.
 *
 * @author yyz
 */
public class RtpSocket extends UdpServerTask {

    private RtcpReportSocket reportSocket;

    /**
     * rtp通讯
     *
     * @param bindAddress 本地监听地址
     * @param rtpPort     音视频数据端口
     * @param rtcpPort    rtcp 端口
     */
    public RtpSocket(String bindAddress, int rtpPort, int rtcpPort) {
        setAddress(bindAddress, rtpPort);
        setSender(new RtpSender());
        reportSocket = new RtcpReportSocket(bindAddress, rtcpPort);
    }

    public int[] getLocalPorts() {
        return new int[]{
                getLocalPort(),
                reportSocket.getLocalPort()
        };
    }

    public int getLocalPort() {
        if (getTaskStatus() == NetTaskStatus.NONE) {
            return 0;
        }
        while (getSocket() == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return getSocket().getLocalPort();
    }

    public RtcpReportSocket getReportSocket() {
        return reportSocket;
    }

//    public void setDestination(int rtpPort, int rtcpPort) {
//        setAddress(getHost(), rtpPort);
//        reportSocket.setAddress(reportSocket.getHost(), rtcpPort);
//    }

//    public void sendRtpPacket(RtpPacket packet, RtcpPacket rtcpPacket) {
//        UdpSender sender = getSender();
//        //发送rtcp数据
//        //reportSocket.update(packet.getLimit(), (packet.getTime() / 100L) * (packet.getClockFrequency() / 1000L) / 10000L);
//        //reportSocket.update(packet.getLimit(), (packet.getTime() + packet.getClockFrequency() / 25) / 10000L);
//        //发送rtp数据
//        //rtpSocket.putSendData(packet.getData(), packet.getLimit());//4-8
////            reportSocket.sendRtcpData(rtcpPacket, packet.getLimit(), packet.getTimeStamp());//4-8
//        sender.sendData(packet);
//    }

}
