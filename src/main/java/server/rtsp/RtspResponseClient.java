package server.rtsp;

import connect.network.nio.*;
import log.LogDog;
import server.rtsp.joggle.IDataSrc;
import server.rtsp.protocol.RtspProtocol;
import server.rtsp.rtp.RtpSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RtspResponseClient extends NioClientTask {

    /**
     * Parse method & uri
     */
    private Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP", Pattern.CASE_INSENSITIVE);
    // Parse a request header
//        private final Pattern regexHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE);

    private RtpSocket videoSocket = null;
    private RtpSocket audioSocket = null;

    private String remoteAddress = null;
//        private int remotePort = 0;

    private String localAddress = null;
    private int localPort = 0;

    private IDataSrc dataSrc;
    private RtspServer server;

    public RtspResponseClient(RtspServer server, IDataSrc dataSrc, SocketChannel socket) {
        super(socket);
        this.dataSrc = dataSrc;
        this.server = server;
        setSender(new NioHPCSender(this));
        setReceive(new NioReceive(this, "onReceiveData"));
    }

    @Override
    protected void onConfigSocket(boolean isConnect, SocketChannel channel) {
        if (isConnect) {
            try {
                InetSocketAddress remote = (InetSocketAddress) getSocketChannel().getRemoteAddress();
                remoteAddress = remote.getAddress().getHostAddress();
                InetSocketAddress local = (InetSocketAddress) getSocketChannel().getLocalAddress();
                localAddress = local.getAddress().getHostAddress();
                localPort = local.getPort();
//                    remotePort = remote.getPort();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 接受数据回调（设置setReceive 传进该方法名）
     *
     * @param data
     */
    private void onReceiveData(byte[] data) {
        Request request;
        try {
            request = parseRequest(data);
        } catch (Exception e) {
            e.printStackTrace();
            request = new Request();
            request.method = new String(data);
        }
        data = processRequest(request);
        LogDog.d("发送给客户端 ：" + new String(data));
        getSender().sendData(data);
    }

    /**
     * 解析请求
     *
     * @param data
     * @return
     * @throws Exception
     */
    private Request parseRequest(byte[] data) throws Exception {
        String str = new String(data);
        if (!str.contains("RTSP/1.0")) {
            throw new SocketException("IllegalArgumentException = " + str);
        }
        LogDog.d("==>接收到RTSP客户端的请求 : " + str);
        Request request = new Request();

        String[] array = str.split("\\r\\n");
        for (String tmp : array) {
            if (tmp.contains("CSeq:")) {
                request.seq = tmp.replace("CSeq:", "").trim();
                break;
            }
        }
        request.content = array;

        Matcher matcher = regexMethod.matcher(str);
        matcher.find();
        request.method = matcher.group(1);
        request.uri = matcher.group(2);
        return request;
    }

    /**
     * 响应请求
     *
     * @param request
     * @return
     */
    public byte[] processRequest(Request request) {
        String response;

        /* ********************************************************************************** */
        /* ********************************* Method DESCRIBE ******************************** */
        /* ********************************************************************************** */
        if (request.method.equalsIgnoreCase("DESCRIBE")) {
            //开启传输数据socket
            videoSocket = new RtpSocket(remoteAddress, 5006, 5007);
            videoSocket.startConnect();
            audioSocket = new RtpSocket(remoteAddress, 5004, 5005);
            audioSocket.startConnect();

            server.notifyStart();
            if (server.getAudioSet().isEmpty() || server.getVideoSet().isEmpty()) {
                dataSrc.setSteamOutSwitch(true);
            }
            server.getAudioSet().add(audioSocket);
            server.getVideoSet().add(videoSocket);

            StringBuilder sb = new StringBuilder();
//                long uptime = System.currentTimeMillis();
//                long mTimestamp = (uptime / 1000) << 32 & (((uptime - ((uptime / 1000) * 1000)) >> 32) / 1000); // NTP timestamp
            sb.append("v=0\r\n");
            // Add IPV6 support
            // sb.append("o=- " + session id + " " + session id + " IN IP4 " + getServerIp() + "\r\n");
            sb.append("o=- " + RtspProtocol.SESSION + " " + RtspProtocol.SESSION + " IN IP4 " + localAddress + "\r\n");
            sb.append("s=Unnamed\r\n");
            sb.append("i=N/A\r\n");
            sb.append("c=IN IP4 " + localAddress + "\r\n");
            sb.append("t=0 0\r\n");
//                sb.append("a=range:npt=0- 596.458\r\n");
            sb.append("a=control:*\r\n");
            sb.append("a=recvonly\r\n");

            //音频信息配置
//                sb.append("m=audio 5004 RTP/AVP 96\r\n");
            //AMR编码
//                sb.append("a=rtpmap:96 AMR/8000\r\n");
            //AAC编码
//                sb.append("a=rtpmap:96 mpeg4-generic/" + dataSrc.getSamplingRate() + "\r\n");
//                sb.append("a=fmtp:96 octet-align=1;\r\n");
//                sb.append("a=fmtp:96 streamtype=5; profile-level-id=1; mode=AAC-hbr; config=" + dataSrc.getConfig() +
//                        ";sizelength=13;indexlength=3;indexdeltalength=3;\r\n");
//                sb.append("a=control:trackID=" + 0 + "\r\n");

            //视频信息配置
            sb.append("m=video 5006 RTP/AVP 96\r\n");
            sb.append("a=rtpmap:96 H264/90000\r\n");
            sb.append("a=fmtp:96 packetization-mode=1;profile-level-id=" + dataSrc.getProfileLevel() +
                    ";sprop-parameter-sets=" + dataSrc.getBase64SPS() + "," + dataSrc.getBase64PPS() + ";\r\n");
//                sb.append("a=cliprect:0,0,160,240" + "\r\n");
//                sb.append("a=framesize:97 240-160" + "\r\n");
            sb.append("a=framerate:30.0" + "\r\n");
            sb.append("a=control:trackID=" + 1 + "\r\n");

            response = RtspProtocol.responseDescribe(localAddress + ":" + localPort, request.seq, sb.toString());

        }
        /* ********************************************************************************** */
        /* ********************************* Method OPTIONS ********************************* */
        /* ********************************************************************************** */
        else if (request.method.equalsIgnoreCase("OPTIONS")) {
            //相当于心跳
            response = RtspProtocol.responseOptions(request.seq);
        }
        /* ********************************************************************************** */
        /* ********************************** Method SETUP ********************************** */
        /* ********************************************************************************** */
        else if (request.method.equalsIgnoreCase("SETUP")) {
            Pattern pattern = Pattern.compile("trackID=(\\w+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(request.uri);
            int rtpPort = 0, rtcpPort = 0;

            if (!matcher.find()) {
                response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST, request.seq);
                return response.getBytes();
            }
            //0 是音频，1 是视频
            int trackId = Integer.parseInt(matcher.group(1));
            if (trackId > 1 || trackId < 0) {
                //非法的trackId
                response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST, request.seq);
                return response.getBytes();
            }

//                int ssrc = trackId == 1 ? videoSocket.getSSRC() : audioSocket.getSSRC();
            int ssrc = trackId == 1 ? server.getVideoRtpPacket().getSSRC() : server.getAudioRtpPacket().getSSRC();
            int src[] = trackId == 1 ? videoSocket.getLocalPorts() : audioSocket.getLocalPorts();

            String transport = null;
            try {
                transport = InetAddress.getByName(localAddress).isMulticastAddress() ? "multicast" : "unicast";
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            String clientPort = null;
            for (String tmp : request.content) {
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
                videoSocket.setDestination(rtpPort, rtcpPort);
            }
            if (trackId == 0) {
                audioSocket.setDestination(rtpPort, rtcpPort);
            }

            String data = "RTP/AVP/UDP;" + transport +
                    ";client_port=" + clientPort +
                    ";source=" + remoteAddress +
                    ";server_port=" + src[0] + "-" + src[1] +
                    ";ssrc=" + Integer.toHexString(ssrc)
//                        + ";mode=play"
                    ;

            response = RtspProtocol.responseSetup(request.seq, data);
        }
        /* ********************************************************************************** */
        /* ********************************** Method PLAY *********************************** */
        /* ********************************************************************************** */
        else if (request.method.equalsIgnoreCase("PLAY")) {
            audioSocket.setPause(false);
            videoSocket.setPause(false);
            response = RtspProtocol.responsePlay(request.seq, localAddress, localPort, true, true);
        }
        /* ********************************************************************************** */
        /* ********************************** Method PAUSE ********************************** */
        /* ********************************************************************************** */
        else if (request.method.equalsIgnoreCase("PAUSE")) {
            audioSocket.setPause(true);
            videoSocket.setPause(true);
            response = RtspProtocol.responsePause(request.seq);
        }
        /* ********************************************************************************** */
        /* ********************************* Method TEARDOWN ******************************** */
        /* ********************************************************************************** */
        else if (request.method.equalsIgnoreCase("TEARDOWN")) {
            NioClientFactory.getFactory().removeTask(this);
            response = RtspProtocol.responseTeardown(request.seq);
        }
        /* ********************************************************************************** */
        /* ********************************* Method GET_PARAMETER *************************** */
        /* ********************************************************************************** */
        else if (request.method.equalsIgnoreCase("GET_PARAMETER")) {
            response = RtspProtocol.responseGetParameter(request.seq);
        }
        /* ********************************************************************************** */
        /* ********************************* Unknown method ? ******************************* */
        /* ********************************************************************************** */
        else {
            response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST, request.seq);
        }
        return response.getBytes();
    }

    @Override
    protected void onCloseSocketChannel() {
        if (server.getAudioSet().isEmpty() && server.getVideoSet().isEmpty()) {
            dataSrc.setSteamOutSwitch(false);
        }
        server.getAudioSet().remove(audioSocket);
        server.getVideoSet().remove(videoSocket);
        if (audioSocket != null) {
            audioSocket.stopConnect();
        }
        if (videoSocket != null) {
            videoSocket.stopConnect();
        }
        LogDog.w("==> RtspServer 断开与" + remoteAddress + "客户端的链接...");
        server.notifyStop();
    }


    private class Request {
        public String seq = null;
        public String method = null;
        public String uri = null;
        public String[] content = null;
    }

}
