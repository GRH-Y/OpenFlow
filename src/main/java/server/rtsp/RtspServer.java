package server.rtsp;


import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerTask;
import server.rtsp.packet.RtcpPacket;
import server.rtsp.packet.RtpPacket;
import util.LogDog;
import util.NetUtils;

import java.nio.channels.SocketChannel;

/**
 * RtspServer rtsp推流服务端
 * （支持多用户同时连接）
 * Created by prolog on 2017/8/9.
 */

public class RtspServer extends NioServerTask {
    private DataSrc dataSrc = null;
    /**
     * 链接的客户端数量
     */
    private int connectNumber = 0;

    private RtpPacket videoRtpPacket;
    private RtpPacket audioRtpPacket;

    private RtcpPacket videoRtcpPacket;
    private RtcpPacket audioRtcpPacket;

    public RtspServer(int port) {
        String ip = NetUtils.getLocalIp("wlan0");
        setAddress(ip, port);

        videoRtpPacket = new RtpPacket();
        audioRtpPacket = new RtpPacket();
        videoRtpPacket.setClockFrequency(90000);
        audioRtpPacket.setClockFrequency(8000);
        videoRtcpPacket = new RtcpPacket(videoRtpPacket.getSSRC());
        audioRtcpPacket = new RtcpPacket(audioRtpPacket.getSSRC());
    }

    public RtspServer(String ip, int port) {
        setAddress(ip, port);
    }

    public void setDataSrc(DataSrc dataSrc) {
        this.dataSrc = dataSrc;
    }

    public RtpPacket getVideoRtpPacket() {
        return videoRtpPacket;
    }

    public RtpPacket getAudioRtpPacket() {
        return audioRtpPacket;
    }

    public RtcpPacket getVideoRtcpPacket() {
        return videoRtcpPacket;
    }

    public RtcpPacket getAudioRtcpPacket() {
        return audioRtcpPacket;
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        connectNumber++;
        LogDog.w("==> RtspServer has client connect , client connect number = " + connectNumber);
        RtspResponseClient client = new RtspResponseClient(this, dataSrc, channel);
        NioClientFactory factory = NioClientFactory.getFactory();
        factory.open();
        factory.addTask(client);
    }


    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
        if (isSuccess) {
            LogDog.v("==> RtspServer running, server address = " + getServerHost());
            dataSrc.startVideoEncode();
            dataSrc.startAudioEncode();
        }
    }


    @Override
    protected void onCloseServerChannel() {
        dataSrc.stopAudioEncode();
        dataSrc.stopVideoEncode();
        LogDog.w("==> RtspServer stopTask ing....");
        NioClientFactory.getFactory().close();
    }

    protected void notifyStop() {
        connectNumber--;
        if (connectNumber <= 0) {
            LogDog.w("==>  当前没有客户端连接，停止音视频线程!");
            dataSrc.stopAudioEncode();
            dataSrc.stopVideoEncode();
        }
    }

}
