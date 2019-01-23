package server.rtsp;


import connect.network.nio.*;
import server.rtsp.packet.RtcpPacket;
import server.rtsp.packet.RtpPacket;
import server.rtsp.protocol.RtspProtocol;
import server.rtsp.rtp.RtpSocket;
import util.LogDog;
import util.NetUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public RtspServer(int port) {
        String ip = NetUtils.getLocalIp("wlan0");
        setAddress(ip, port);
    }

    public RtspServer(String ip, int port) {
        setAddress(ip, port);
    }

    public void setDataSrc(DataSrc dataSrc) {
        this.dataSrc = dataSrc;
    }


//    @Override
//    protected void onAcceptTimeoutServer() {
//        //LogDog.v("==> RtspServer running, server address = " + getIpAndPort() + " client connect number = " + connectCache.size());
//    }


    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        LogDog.w("==> RtspServer has client connect come...");
        Client client = new Client(channel);
        NioClientFactory factory = NioClientFactory.getFactory();
        factory.open();
        factory.addTask(client);
        connectNumber++;
    }


    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
        if (isSuccess) {
            LogDog.v("==> RtspServer running, server address = " + getServerHost() + " client connect number = " + connectNumber);
//            dataSrc.startVideoEncode();
//            dataSrc.startAudioEncode();
        }
    }


    @Override
    protected void onCloseServerChannel() {
        dataSrc.stopAudioEncode();
        dataSrc.stopVideoEncode();
        LogDog.w("==> RtspServer stopTask ing....");
        NioClientFactory.getFactory().close();
    }

    private void notifyStop() {
        if (connectNumber == 0) {
            LogDog.w("==>  当前没有客户端连接，停止音视频线程!");
            dataSrc.stopAudioEncode();
            dataSrc.stopVideoEncode();
        }
    }


    private class Client extends NioClientTask {
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

        public Client(SocketChannel socket) {
            super(socket);
            setSender(new NioSender());
            setReceive(new NioReceive(this, "onReceiveData"));
        }


        @Override
        protected void onConnectSocketChannel(boolean isConnect) {
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

//            matcher = regexHeader.matcher(str);
//            matcher.find();
//            request.headers.put(matcher.group(1).toLowerCase(Locale.US), matcher.group(2));
//            LogDog.i("==> parseRequest : " + " method = " + request.method + " seq = " + request.seq);
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
                //初始化
                RtpPacket videoRtpPacket = new RtpPacket();
                RtpPacket audioRtpPacket = new RtpPacket();
                videoRtpPacket.setClockFrequency(90000);
                audioRtpPacket.setClockFrequency(8000);
                dataSrc.setAudioRtpPacket(audioRtpPacket);
                dataSrc.setVideoRtpPacket(videoRtpPacket);
                RtcpPacket videoRtcpPacket = new RtcpPacket(videoRtpPacket.getSSRC());
                RtcpPacket audioRtcpPacket = new RtcpPacket(audioRtpPacket.getSSRC());
                dataSrc.setVideoRtcpPacket(videoRtcpPacket);
                dataSrc.setAudioRtcpPacket(audioRtcpPacket);
                // 开启录制音视频设备
                dataSrc.startVideoEncode();
                dataSrc.startAudioEncode();
                //开启传输数据socket
                videoSocket = new RtpSocket(remoteAddress, 5006, 5007);
                videoSocket.startConnect();
//                dataSrc.addVideoSocket(videoSocket);
//                videoSocket.setClockFrequency(90000);
                audioSocket = new RtpSocket(remoteAddress, 5004, 5005);
                audioSocket.startConnect();
//                dataSrc.addAudioSocket(audioSocket);
//                audioSocket.setClockFrequency(8000);

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
                sb.append("a=framerate:24.0" + "\r\n");
                sb.append("a=control:trackID=" + 1 + "\r\n");

                response = RtspProtocol.responseDescribe(localAddress + ":" + localPort, request.seq, sb.toString());

            }

            /* ********************************************************************************** */
            /* ********************************* Method OPTIONS ********************************* */
            /* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("OPTIONS")) {
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
                int ssrc = trackId == 1 ? dataSrc.getVideoRtpPacket().getSSRC() : dataSrc.getAudioRtpPacket().getSSRC();
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
//                    dataSrc.startVideoEncode();
                    videoSocket.setDestination(rtpPort, rtcpPort);
                }
                if (trackId == 0) {
//                    dataSrc.startAudioEncode();
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
                dataSrc.addVideoSocket(videoSocket);
                dataSrc.addAudioSocket(audioSocket);
//                audioSocket.continueConnect(true);
//                videoSocket.continueConnect(true);
                response = RtspProtocol.responsePlay(request.seq, localAddress, localPort, true, true);
            }

            /* ********************************************************************************** */
            /* ********************************** Method PAUSE ********************************** */
            /* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PAUSE")) {
//                audioSocket.pauseConnect();
//                videoSocket.pauseConnect();
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
            dataSrc.removeAudioSocket(audioSocket);
            dataSrc.removeVideoSocket(videoSocket);
            if (audioSocket != null) {
                audioSocket.stopConnect();
            }
            if (videoSocket != null) {
                videoSocket.stopConnect();
            }
            notifyStop();
            LogDog.w("==> RtspServer 断开与" + remoteAddress + "客户端的链接...");
        }


        private class Request {
            public String seq = null;
            public String method = null;
            public String uri = null;
            public String[] content = null;
        }

    }


}
