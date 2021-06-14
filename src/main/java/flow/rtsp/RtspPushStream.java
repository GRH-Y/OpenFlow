package flow.rtsp;

import connect.network.base.joggle.INetSender;
import connect.network.base.joggle.ISenderFeedback;
import flow.avdecode.audio.AACAudioStream;
import flow.avdecode.video.H264VideoStream;
import flow.core.FlowDataSrc;
import flow.core.FlowPushStream;
import flow.core.OpenFlow;
import flow.rtsp.packet.NalPacket;
import flow.rtsp.packet.PacketType;
import flow.rtsp.packet.RtpPacket;
import flow.rtsp.rtp.RtpSender;
import flow.rtsp.rtp.RtpSocket;

import java.net.DatagramPacket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RtspPushStream extends FlowPushStream<DatagramPacket, NalPacket> implements ISenderFeedback {


    private RtpSocket videoSocket;
    private RtpSocket audioSocket;

    private H264VideoStream videoStream;
    private AACAudioStream audioStream;

    private volatile AtomicInteger seq = new AtomicInteger(-1);

    public RtspPushStream(OpenFlow openFlow) {
        super(openFlow);
        FlowDataSrc dataSrc = openFlow.getFlowDataSrc();
        videoStream = dataSrc.getVideoStream();
        audioStream = dataSrc.getAudioStream();
    }

    public void configRtpSocket(RtpSocket videoSocket, RtpSocket audioSocket) {
        this.videoSocket = videoSocket;
        this.audioSocket = audioSocket;
        RtpSender videoSender = videoSocket.getSender();
        videoSender.setRtpClientList(videoClientList);
        videoSender.setSenderFeedback(this);
        RtpSender audioSender = audioSocket.getSender();
        audioSender.setRtpClientList(audioClientList);
        audioSender.setSenderFeedback(this);
    }


    private int getSeq() {
        synchronized (RtspPushStream.class) {
            return seq.incrementAndGet();
        }
    }

    private void sendRtpData(RtpSocket socket, RtpPacket rtpPacket, List<DatagramPacket> packetList) {
        if (packetList.isEmpty()) {
            return;
        }
        socket.getSender().sendData(rtpPacket);
    }

    @Override
    public void onStreamDataPush(NalPacket packet) {
        if (PacketType.AUDIO == packet.getPacketType()) {
            RtpPacket audioRtpPacket = packet.toRtpPacket(getSeq(), audioStream.getSSRC());
            synchronized (audioClientList) {
                sendRtpData(audioSocket, audioRtpPacket, audioClientList);
            }
        } else {
            RtpPacket videoRtpPacket = packet.toRtpPacket(getSeq(), videoStream.getSSRC());
            synchronized (videoClientList) {
                sendRtpData(videoSocket, videoRtpPacket, videoClientList);
            }
        }
    }

    @Override
    public void onSenderFeedBack(INetSender iNetSender, Object data, Throwable throwable) {
        if (data instanceof RtpPacket) {
            RtpPacket packet = (RtpPacket) data;
            NalPacket nalPacket = packet.getNalPacket();
            if (PacketType.AUDIO == nalPacket.getPacketType()) {
                audioStream.resetNalPacket(nalPacket);
            } else {
                videoStream.resetNalPacket(nalPacket);
            }
        }
    }
}
