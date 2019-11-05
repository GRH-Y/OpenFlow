package server.rtsp;


import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerTask;
import log.LogDog;
import server.rtsp.packet.RtcpPacket;
import server.rtsp.packet.RtpPacket;
import server.rtsp.rtp.RtpSocket;
import util.NetUtils;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

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

    private List<RtpSocket> videoSet;
    private List<RtpSocket> audioSet;

    public RtspServer(int port) {
        String ip = NetUtils.getLocalIp("wlan0");
        setAddress(ip, port);

        videoSet = new ArrayList<>();
        audioSet = new ArrayList<>();
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

    public List<RtpSocket> getAudioSet() {
        return audioSet;
    }

    public List<RtpSocket> getVideoSet() {
        return videoSet;
    }

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        connectNumber++;
        LogDog.w("==> RtspServer has client connect , client connect number = " + connectNumber);
        RtspResponseClient client = new RtspResponseClient(this, dataSrc, channel);
        NioClientFactory.getFactory().open();
        NioClientFactory.getFactory().addTask(client);
    }

    @Override
    protected void onConfigServer(boolean isSuccess, ServerSocketChannel channel) {
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

    protected void notifyStart() {
        dataSrc.startAudioEncode();
        dataSrc.startVideoEncode();
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
