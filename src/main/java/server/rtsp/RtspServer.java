package server.rtsp;


import connect.network.nio.*;
import server.rtsp.protocol.RtspProtocol;
import server.rtsp.rtp.RtpSocket;
import util.Logcat;
import util.NetUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RtspServer rtsp推流服务端
 * （支持多用户同时连接）
 * Created by prolog on 2017/8/9.
 */

public class RtspServer extends NioServerTask {
    //    private ISessionCallBack callBack;
    private DataSrc dataSrc = null;
    /**
     * 链接的客户端
     */
    private Queue<Client> connectCache = new ConcurrentLinkedQueue<>();

    public RtspServer(int port) {
        String ip = NetUtils.getLocalIp("wlan0");
        setAddress(ip, port);
//        callBack = new JavSessionCallBack(this);
    }

    public RtspServer(int port, String netInterface) throws SocketException {
        String ip = NetUtils.getLocalIp(netInterface);
        setAddress(ip, port);
//        callBack = new JavSessionCallBack(this);
    }

    public void setDataSrc(DataSrc dataSrc) {
        this.dataSrc = dataSrc;
    }

//    @Override
//    protected void onAcceptTimeout() {
//        Logcat.v("==> RtspServer running, port = " + getServerPort() + " client connect number = " + connectCache.size());
//    }


    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        Logcat.w("==> RtspServer has client connect come...");
        Client client = new Client(channel);
        NioClientFactory factory = NioClientFactory.getFactory();
        factory.open();
        factory.addTask(client);
        connectCache.add(client);
    }


    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
//        if (isSuccess) {
//            dataSrc.startVideoEncode();
//            dataSrc.startAudioEncode();
//        }
    }


    @Override
    protected void onCloseServerChannel() {
        dataSrc.stopAudioEncode();
        dataSrc.stopVideoEncode();
        Logcat.w("==> RtspServer stopTask ing....");
        for (Client connect : connectCache) {
            NioClientFactory factory = NioClientFactory.getFactory();
            factory.open();
            factory.removeTask(connect);
        }
        connectCache.clear();
    }

    private void notifyStop(NioClientTask connect) {
        connectCache.remove(connect);
        if (connectCache.size() == 0) {
            Logcat.w("==>  当前没有客户端连接，停止音视频线程!");
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
            Logcat.d("发送给客户端 ：" + new String(data));
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
            Logcat.d("==>接收到RTSP客户端的请求 : " + str);
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
//            Logcat.i("==> parseRequest : " + " method = " + request.method + " seq = " + request.seq);
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
                // 开启录制音视频设备
                dataSrc.startVideoEncode();
                dataSrc.startAudioEncode();
                //开启传输数据socket
                videoSocket = new RtpSocket(remoteAddress, 5006, 5007);
                videoSocket.startConnect();
//                dataSrc.addVideoSocket(videoSocket);
                videoSocket.setClockFrequency(90000);
                audioSocket = new RtpSocket(remoteAddress, 5004, 5005);
                audioSocket.startConnect();
//                dataSrc.addAudioSocket(audioSocket);
                audioSocket.setClockFrequency(8000);

                StringBuilder sb = new StringBuilder();
//                long uptime = System.currentTimeMillis();
//                long mTimestamp = (uptime / 1000) << 32 & (((uptime - ((uptime / 1000) * 1000)) >> 32) / 1000); // NTP timestamp
                sb.append("v=0\r\n");
                // Add IPV6 support
                // sb.append("o=- " + mTimestamp + " " + mTimestamp + " IN IP4 " + getServerIp() + "\r\n");
                sb.append("o=- 0 0 IN IP4 " + localAddress + "\r\n");
                sb.append("s=Unnamed\r\n");
                sb.append("i=N/A\r\n");
                sb.append("c=IN IP4 " + remoteAddress + "\r\n");
                // t=0 0 means the session is permanent (we don't know when it will stopTask)
                sb.append("t=0 0\r\n");
                sb.append("a=recvonly\r\n");

                // Prevents two different sessions from using the same peripheral at the same time
                sb.append("m=audio 5004 RTP/AVP 96\r\n");
                //AMR编码
//                sb.append("a=rtpmap:96 AMR/8000\r\n");
//                sb.append("a=fmtp:96 octet-align=1;\r\n");
                //AAC编码
                sb.append("a=rtpmap:96 mpeg4-generic/" + dataSrc.getSamplingRate() + "\r\n");
                sb.append("a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=" + dataSrc.getConfig() +
                        "; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n");

                sb.append("a=control:trackID=" + 0 + "\r\n");
                sb.append("m=video 5006 RTP/AVP 96\r\n");
                sb.append("a=rtpmap:96 H264/90000\r\n");
                sb.append("a=fmtp:96 packetization-mode=1;profile-level-id=" + dataSrc.getProfileLevel() +
                        ";sprop-parameter-sets=" + dataSrc.getBase64SPS() + "," + dataSrc.getBase64PPS() + ";\r\n");
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
                    response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST);
                    return response.getBytes();
                }
                //0 是音频，1 是视频
                int trackId = Integer.parseInt(matcher.group(1));
                if (trackId > 1 || trackId < 0) {
                    //非法的trackId
                    response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST);
                    return response.getBytes();
                }

                int ssrc = trackId == 1 ? videoSocket.getSSRC() : audioSocket.getSSRC();
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
                    dataSrc.startVideoEncode();
                    videoSocket.setDestination(rtpPort, rtcpPort);
                }
                if (trackId == 0) {
                    dataSrc.startAudioEncode();
                    audioSocket.setDestination(rtpPort, rtcpPort);
                }

                String data = "RTP/AVP/UDP;" + transport +
                        ";destination=" + remoteAddress +
                        ";client_port=" + clientPort +
                        ";server_port=" + src[0] + "-" + src[1] +
                        ";ssrc=" + Integer.toHexString(ssrc) +
                        ";mode=play";


                response = RtspProtocol.responseSetup(request.seq, data);
            }

            /* ********************************************************************************** */
            /* ********************************** Method PLAY *********************************** */
            /* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PLAY")) {
                dataSrc.addVideoSocket(videoSocket);
                dataSrc.addAudioSocket(audioSocket);
                audioSocket.continueConnect(true);
                videoSocket.continueConnect(true);
                response = RtspProtocol.responsePlay(request.seq, localAddress, localPort, true, true);
            }

            /* ********************************************************************************** */
            /* ********************************** Method PAUSE ********************************** */
            /* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("PAUSE")) {
                audioSocket.pauseConnect();
                videoSocket.pauseConnect();
                response = RtspProtocol.responsePause(request.seq);
            }

            /* ********************************************************************************** */
            /* ********************************* Method TEARDOWN ******************************** */
            /* ********************************************************************************** */
            else if (request.method.equalsIgnoreCase("TEARDOWN")) {
                NioClientFactory factory = NioClientFactory.getFactory();
                factory.removeTask(this);
                response = RtspProtocol.responseTeardown(request.seq);
            }

            /* ********************************************************************************** */
            /* ********************************* Unknown method ? ******************************* */
            /* ********************************************************************************** */
            else {
                response = RtspProtocol.responseError(RtspProtocol.STATUS_BAD_REQUEST);
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
            notifyStop(this);
            Logcat.w("==> RtspServer 断开与" + remoteAddress + "客户端的链接...");
        }


        private class Request {
            public String seq = null;
            public String method = null;
            public String uri = null;
            public String[] content = null;
        }

    }


}
