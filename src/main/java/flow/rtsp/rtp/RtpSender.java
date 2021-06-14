package flow.rtsp.rtp;

import connect.network.udp.UdpSender;
import flow.rtsp.packet.NalPacket;
import flow.rtsp.packet.RtpPacket;

import java.net.DatagramPacket;
import java.util.List;

public class RtpSender extends UdpSender {

    private List<DatagramPacket> rtpClientList;

    private List<DatagramPacket> rtcpClientList;

    public void setRtpClientList(List<DatagramPacket> rtpClientList) {
        this.rtpClientList = rtpClientList;
    }

    public void setRtcpClientList(List<DatagramPacket> rtcpClientList) {
        this.rtcpClientList = rtcpClientList;
    }

    @Override
    protected int onHandleSendData(Object objData) throws Throwable {
        if (objData instanceof RtpPacket) {
            RtpPacket rtpPacket = (RtpPacket) objData;
            NalPacket nalPacket = rtpPacket.getNalPacket();
            for (DatagramPacket target : rtpClientList) {
                target.setData(nalPacket.getData());
                socket.send(target);
            }
            return 0;
        } else if (objData instanceof byte[]) {
            byte[] rtcpData = (byte[]) objData;
            for (DatagramPacket target : rtcpClientList) {
                target.setData(rtcpData);
                socket.send(target);
            }
            return 0;
        }
        return super.onHandleSendData(objData);
    }
}
