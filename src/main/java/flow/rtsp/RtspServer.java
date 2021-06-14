package flow.rtsp;


import connect.network.base.NetTaskStatus;
import flow.core.FlowClient;
import flow.core.FlowNetFactory;
import flow.core.FlowServer;
import flow.rtsp.rtp.RtpSocket;
import log.LogDog;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * RtspServer rtsp推流服务端
 * （支持多用户同时连接）
 * Created by prolog on 2017/8/9.
 */

public class RtspServer extends FlowServer {

    /**
     * 链接的客户端数量
     */
    private volatile int connectCount = 0;

    /**
     * 视频rtp端口
     */
    private int videoRtpPort = 5006;
    /**
     * 音频rtp端口
     */
    private int audioRtpPort = 5004;

    private RtpSocket videoSocket = null;
    private RtpSocket audioSocket = null;

    /**
     * 设置rtp音视频输出端口
     *
     * @param videoPort 视频端口
     * @param audioPort 音频端口
     */
    public void setRtpPort(int videoPort, int audioPort) {
        if (videoPort <= 0 || audioPort <= 0) {
            throw new IllegalArgumentException(" videoPort or audioPort less than 0 !!!");
        }
        if (getTaskStatus() == NetTaskStatus.RUN) {
            throw new IllegalStateException("Can only be set before startup !!!");
        }
        this.videoRtpPort = videoPort;
        this.audioRtpPort = audioPort;
    }

    public RtpSocket getAudioSocket() {
        return audioSocket;
    }

    public RtpSocket getVideoSocket() {
        return videoSocket;
    }

    @Override
    protected FlowClient onAccept(SocketChannel channel) {
        synchronized (this) {
            connectCount++;
        }
        LogDog.w("==> RtspServer has client connect , client connect number = " + connectCount);
        RtspResponseClient client = new RtspResponseClient(channel, openFlow);
        return client;
    }

    @Override
    protected void onBootServerComplete(ServerSocketChannel channel) {
        LogDog.v("==> RtspServer running, server address = " + getHost());

        FlowNetFactory netFactory = openFlow.getFlowNetFactory();
        videoSocket = new RtpSocket(getHost(), videoRtpPort, videoRtpPort + 1);
        netFactory.addUdpClient(videoSocket);
        netFactory.addUdpClient(videoSocket.getReportSocket());
        audioSocket = new RtpSocket(getHost(), audioRtpPort, audioRtpPort + 1);
        netFactory.addUdpClient(audioSocket);
        netFactory.addUdpClient(audioSocket.getReportSocket());

        RtspPushStream pushStream = openFlow.getFlowPushStream();
        pushStream.configRtpSocket(videoSocket, audioSocket);
    }

    @Override
    protected void onCloseServerChannel() {
        super.onCloseServerChannel();
        LogDog.w("==> RtspServer stopTask ing....");
    }

    @Override
    public void onTaskState(NetTaskStatus netTaskStatus) {
        if (netTaskStatus == NetTaskStatus.NONE) {
            //任务执行结束
            synchronized (this) {
                connectCount--;
            }
            LogDog.w("==> RtspServer has client disConnect , client connect number = " + connectCount);
        }
    }
}
