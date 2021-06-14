package flow.rtsp;

import connect.network.base.joggle.INetReceiver;
import connect.network.nio.NioReceiver;
import connect.network.nio.NioSender;
import connect.network.xhttp.utils.MultiLevelBuf;
import flow.avdecode.NalStream;
import flow.avdecode.audio.AACAudioStream;
import flow.avdecode.video.H264VideoStream;
import flow.core.*;
import flow.rtsp.packet.RtcpPacket;
import flow.rtsp.protocol.RtspProtocol;
import flow.rtsp.rtp.RtpSocket;
import log.LogDog;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtspResponseClient extends FlowClient implements INetReceiver<MultiLevelBuf> {

    /**
     * Parse method & uri
     */
    private Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP", Pattern.CASE_INSENSITIVE);
    // Parse a request header
//        private final Pattern regexHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE);

    private DatagramPacket rtpVideoPacket = null;
    private DatagramPacket rtpAudioPacket = null;

    private String remoteAddress = null;
    private int remotePort = 0;

    private String session = null;
    private String version = null;

    private final int audio_trackId = 0;
    private final int video_trackId = 1;

    private FlowNetFactory flowNetFactory;
    private RtspServer rtspServer;
    private FlowPushStream pushStream;
    private FlowDataSrc dataSrc;
    private H264VideoStream videoStream;
    private AACAudioStream audioStream;

    public RtspResponseClient(SocketChannel channel, OpenFlow openFlow) {
        super(channel, openFlow);
        setChannel(channel);
        flowNetFactory = openFlow.getFlowNetFactory();
        rtspServer = flowNetFactory.getFlowServer();
        pushStream = openFlow.getFlowPushStream();
        dataSrc = openFlow.getFlowDataSrc();
        videoStream = dataSrc.getVideoStream();
        audioStream = dataSrc.getAudioStream();
    }


    @Override
    protected void onConnectCompleteChannel(SocketChannel channel) {
        try {
            InetSocketAddress remote = (InetSocketAddress) getChannel().getRemoteAddress();
            remoteAddress = remote.getAddress().getHostAddress();
            remotePort = remote.getPort();
            LogDog.w("==> new client " + remoteAddress + ":" + remotePort + " connect ...");
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSender(new NioSender(getSelectionKey(), channel));
        NioReceiver receiver = new NioReceiver();
        receiver.setDataReceiver(this);
        setReceive(receiver);
    }


    /**
     * 接受数据回调（设置setReceive 传进该方法名）
     *
     * @param buf
     */
    @Override
    public void onReceiveFullData(MultiLevelBuf buf, Throwable throwable) {
        byte[] data = buf.array();
        if (data == null) {
            return;
        }
        RtspRequest request;
        try {
            request = parseRequest(data);
        } catch (Exception e) {
            e.printStackTrace();
            request = new RtspRequest();
            request.setMethod(new String(data));
        }
        data = processRequest(request);
        LogDog.d("发送给客户端 ：" + new String(data));
        getSender().sendData(data);
        getReceiver().resetMultilevelBuf(buf);
    }

    /**
     * 解析请求
     *
     * @param data
     * @return
     * @throws Exception
     */
    private RtspRequest parseRequest(byte[] data) throws Exception {
        String str = new String(data);
        if (!str.contains("RTSP/1.0")) {
            throw new SocketException("IllegalArgumentException = " + str);
        }
        LogDog.d("==>接收到RTSP客户端的请求 : " + str);
        RtspRequest request = new RtspRequest();

        String[] array = str.split("\\r\\n");
        for (String tmp : array) {
            if (tmp.contains(RtspProtocol.CSEQ)) {
                request.setSeq(tmp.replace(RtspProtocol.CSEQ, "").trim());
                break;
            }
        }
        request.setContent(array);

        Matcher matcher = regexMethod.matcher(str);
        matcher.find();
        request.setMethod(matcher.group(1));
        request.setUrl(matcher.group(2));
        return request;
    }


    private void configPort(DatagramPacket datagramPacket, NalStream stream, int rtpPort, int rtcpPort) {
        try {
            datagramPacket.setAddress(Inet4Address.getByName(remoteAddress));
            datagramPacket.setPort(rtpPort);
            pushStream.addVideoClient(datagramPacket);
            RtcpPacket rtcpPacket = stream.getRtcpPacket();
            rtcpPacket.setPort(rtcpPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 响应请求
     *
     * @param request
     * @return
     */
    public byte[] processRequest(RtspRequest request) {
        if (!dataSrc.isStreamRun()) {
            flowNetFactory.removeTcpClient(this);
            if (RtspProtocol.TEARDOWN.equals(request.getMethod())) {
                return RtspProtocol.responseTeardown(request.getSeq()).getBytes();
            }
            return RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST, request.getSeq()).getBytes();
        }

        String response;
        switch (request.getMethod()) {
            case RtspProtocol.OPTIONS:
                //相当于心跳
                response = RtspProtocol.responseOptions(request.getSeq());
                break;
            case RtspProtocol.DESCRIBE:

                long time = System.currentTimeMillis() / 1000;
                session = String.valueOf(time);
                version = String.valueOf(time + 1);

                byte[] buf = new byte[1];
                rtpVideoPacket = new DatagramPacket(buf, 1);
                rtpAudioPacket = new DatagramPacket(buf, 1);

                StringBuilder sdp = new StringBuilder();
//                long uptime = System.currentTimeMillis();
//                long mTimestamp = (uptime / 1000) << 32 & (((uptime - ((uptime / 1000) * 1000)) >> 32) / 1000); // NTP timestamp
                //v=<version> (协议版本)
                sdp.append("v=0\r\n");
                // Add IPV6 support
                // sb.append("o=- " + session id + " " + session id + " IN IP4 " + getServerIp() + "\r\n");
                //o=<username> <session id> <version> <network type> <address type> <address> (所有者/创建者和会话标识符)
                sdp.append("o=- " + session + " " + version + " IN IP4 " + rtspServer.getHost() + "\r\n");
                sdp.append("s=OpenFlow Sdp \r\n");
                sdp.append("i=OpenFlow v1.0\r\n");
                sdp.append("c=IN IP4 " + rtspServer.getHost() + "\r\n");
                sdp.append("t=0 0\r\n");
//                sb.append("a=range:npt=0- 596.458\r\n");
                sdp.append("a=control:*\r\n");
                sdp.append("a=recvonly\r\n");

                //音频信息配置
                sdp.append("m=audio 0 RTP/AVP 97\r\n");
                //AMR编码
                sdp.append("a=rtpmap:97 AMR/8000\r\n");
                //AAC编码
                sdp.append("a=rtpmap:97 mpeg4-generic/" + audioStream.getSamplingRate() + "\r\n");
                sdp.append("a=fmtp:97 octet-align=1;\r\n");
                sdp.append("a=fmtp:97 streamtype=5; profile-level-id=1; mode=AAC-hbr; config=" + audioStream.getConfig() +
                        ";sizelength=13;indexlength=3;indexdeltalength=3;\r\n");
                sdp.append("a=control:trackID=" + audio_trackId + "\r\n");

                //视频信息配置
                sdp.append("m=video 0 RTP/AVP 96\r\n");
                sdp.append("a=rtpmap:96 H264/90000\r\n");
                sdp.append("a=fmtp:96 packetization-mode=1;profile-level-id=" + videoStream.getProfileLevel() +
                        ";sprop-parameter-sets=" + videoStream.getBase64SPS() + "," + videoStream.getBase64PPS() + "\r\n");
//                sb.append("a=cliprect:0,0,160,240" + "\r\n");
//                sb.append("a=framesize:97 240-160" + "\r\n");
                sdp.append("a=framerate:30.0" + "\r\n");
                sdp.append("a=control:trackID=" + video_trackId + "\r\n");

                String curAddress = rtspServer.getHost() + ":" + getPort();
                response = RtspProtocol.responseDescribe(session, curAddress, request.getSeq(), sdp.toString());
                break;
            case RtspProtocol.SETUP:
                Pattern pattern = Pattern.compile("trackID=(\\w+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(request.getUrl());
                int rtpPort = 0, rtcpPort = 0;

                if (!matcher.find()) {
                    response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST, request.getSeq());
                    return response.getBytes();
                }
                //0 是音频，1 是视频
                int trackId = Integer.parseInt(matcher.group(1));
                if (trackId > video_trackId || trackId < audio_trackId) {
                    //非法的trackId
                    response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST, request.getSeq());
                    return response.getBytes();
                }
//                int ssrc = trackId == 1 ? videoSocket.getSSRC() : audioSocket.getSSRC();
                int ssrc = trackId == video_trackId ? videoStream.getSSRC() : audioStream.getSSRC();
                RtpSocket videoSocket = rtspServer.getVideoSocket();
                RtpSocket audioSocket = rtspServer.getAudioSocket();
                int src[] = trackId == video_trackId ? videoSocket.getLocalPorts() : audioSocket.getLocalPorts();

                String transport = null;
                try {
                    transport = InetAddress.getByName(rtspServer.getHost()).isMulticastAddress() ? "multicast" : "unicast";
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                String clientPort = null;
                for (String tmp : request.getContent()) {
                    if (tmp.contains("Transport:")) {
                        String[] array = tmp.split("=");
                        clientPort = array[1];
                        String[] ports = clientPort.split("-");
                        rtpPort = Integer.parseInt(ports[0]);
                        rtcpPort = Integer.parseInt(ports[1]);
                        break;
                    }
                }

                if (trackId == 1) {
                    configPort(rtpVideoPacket, videoStream, rtpPort, rtcpPort);
                }
                if (trackId == 0) {
                    configPort(rtpAudioPacket, audioStream, rtpPort, rtcpPort);
                }

                String data = "RTP/AVP/UDP;" + transport +
                        ";client_port=" + clientPort +
                        ";source=" + remoteAddress +
                        ";server_port=" + src[0] + "-" + src[1] +
                        ";ssrc=" + Integer.toHexString(ssrc);

                response = RtspProtocol.responseSetup(session, request.getSeq(), data);
                break;
            case RtspProtocol.PLAY:
                videoStream.resumeSteam();
                audioStream.resumeSteam();

                StringBuilder rtpInfo = new StringBuilder();
                rtpInfo.append("RTP-Info: ");

                rtpInfo.append("url=flow.rtsp://");
                rtpInfo.append(rtspServer.getHost());
                rtpInfo.append(":");
                rtpInfo.append(rtspServer.getPort());
                rtpInfo.append("/trackID=" + audio_trackId + ";seq=1;rtptime=0,");

                rtpInfo.append("url=flow.rtsp://");
                rtpInfo.append(rtspServer.getHost());
                rtpInfo.append(":");
                rtpInfo.append(rtspServer.getPort());
                rtpInfo.append("/trackID=" + video_trackId + ";seq=1;rtptime=0");

                response = RtspProtocol.responsePlay(session, request.getSeq(), rtpInfo.toString());
                break;
            case RtspProtocol.PAUSE:
                videoStream.pauseSteam();
                audioStream.pauseSteam();
                response = RtspProtocol.responsePause(request.getSeq());
                break;
            case RtspProtocol.TEARDOWN:
                flowNetFactory.removeTcpClient(this);
                response = RtspProtocol.responseTeardown(request.getSeq());
                break;
            case "GET_PARAMETER":
                response = RtspProtocol.responseGetParameter(request.getSeq());
                break;
            default:
                response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST, request.getSeq());
                break;
        }
        return response.getBytes();
    }


    @Override
    protected void onCloseClientChannel() {
        pushStream.removeAudioClient(rtpAudioPacket);
        pushStream.removeVideoClient(rtpVideoPacket);

        rtspServer = null;
        flowNetFactory = null;
        pushStream = null;
        dataSrc = null;
        videoStream = null;
        audioStream = null;

        LogDog.w("==> client " + remoteAddress + " disconnect ...");
    }
}
