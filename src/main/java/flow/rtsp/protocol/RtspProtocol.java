package flow.rtsp.protocol;

/**
 * Created by Dell on 8/9/2017.
 */

public class RtspProtocol {
    public static final String VENDOR = "YDZ RTSP Server";
    private static final String VERSION = " RTSP/1.0/r/n";
    public static final String STATUS_OK = "RTSP/1.0 200 OK";
    public static final String STATUS_BAD_REQUEST = "RTSP/1.0 500 Server Error";
    private static final String USER_AGENT = "User-Agent: YDZ Streaming Media v2017.08.10 \r\n";
    private static final String SERVER = "Server: " + VENDOR;
    private static int seq = 2;
    private static String DEFAULT_TIMEOUT = ";timeout=60";

    private static final String SPACE = " ";
    private static final String NEXT_LINE = "\r\n";
    public static final String CSEQ = "CSeq: ";
    public static final String SESSION = "Session: ";

    public static final String TEARDOWN = "TEARDOWN";
    public static final String PLAY = "PLAY";
    public static final String SETUP = "SETUP";
    public static final String DESCRIBE = "DESCRIBE";
    public static final String OPTIONS = "OPTIONS";
    public static final String PAUSE = "PAUSE";

    private RtspProtocol() {
    }

    public static String requestTeardown(String address, String sessionId) {
        StringBuilder sb = new StringBuilder();
        sb.append(TEARDOWN);
        sb.append(SPACE);
        sb.append(address);
        sb.append("/");
        sb.append(VERSION);
        sb.append(NEXT_LINE);
        sb.append(CSEQ);
        sb.append(seq++);
        sb.append(NEXT_LINE);
        sb.append(USER_AGENT);
        sb.append(SESSION);
        sb.append(sessionId);
        sb.append(NEXT_LINE);
        sb.append(USER_AGENT);
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    public static String requestPlay(String address, String sessionId, long time) {
        StringBuilder sb = new StringBuilder();
        sb.append(PLAY);
        sb.append(SPACE);
        sb.append(address);
        sb.append(VERSION);
        sb.append(NEXT_LINE);
        sb.append(SESSION);
        sb.append(sessionId);
        sb.append(CSEQ);
        sb.append(seq++);
        sb.append(NEXT_LINE);
        sb.append(USER_AGENT);
        sb.append("Range: npt=");
        sb.append(time);
        sb.append("-");
        sb.append(NEXT_LINE);
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    public static String requestSetup(String address, String trackID, int vPort, int aPort) {
        StringBuilder sb = new StringBuilder();
        sb.append(SETUP);
        sb.append(SPACE);
        sb.append(address);
        sb.append("/");
        sb.append("trackID=");
        sb.append(trackID);
        sb.append(VERSION);
        sb.append(NEXT_LINE);
        sb.append(CSEQ);
        sb.append(seq++);
        sb.append(NEXT_LINE);
        sb.append("Transport: RTP/AVP;unicast;client_port=");//RTP/AVP;UNICAST;client_port=16264-16265;mode=requestPlay/r/n
        sb.append(vPort);
        sb.append("-");
        sb.append(aPort);
        sb.append(NEXT_LINE);
        sb.append(USER_AGENT);
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    public static String requestOptions(String address) {
        StringBuilder sb = new StringBuilder();
        sb.append(OPTIONS);
        sb.append(SPACE);
        sb.append(address);
        sb.append(VERSION);
        sb.append(NEXT_LINE);
        sb.append(CSEQ);
        sb.append(seq++);
        sb.append(NEXT_LINE);
        sb.append(USER_AGENT);
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    public static String requestDescribe(String address) {
        StringBuilder sb = new StringBuilder();
        sb.append(DESCRIBE);
        sb.append(SPACE);
        sb.append(address);
        sb.append(VERSION);
        sb.append(NEXT_LINE);
        sb.append(CSEQ);
        sb.append(seq++);
        sb.append(NEXT_LINE);
        sb.append(USER_AGENT);
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    public static String requestPause(String address, String sessionId) {
        StringBuilder sb = new StringBuilder();
        sb.append(PAUSE);
        sb.append(SPACE);
        sb.append(address);
        sb.append("/");
        sb.append(VERSION);
        sb.append(NEXT_LINE);
        sb.append(CSEQ);
        sb.append(seq++);
        sb.append(NEXT_LINE);
        sb.append(SESSION);
        sb.append(sessionId);
        sb.append(NEXT_LINE);
        sb.append(USER_AGENT);
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    //==============================下面是服务端回应========================================

    private static StringBuilder createResponse(String seq, boolean isNoData) {
        StringBuilder sb = new StringBuilder();
        sb.append(STATUS_OK);
        sb.append(NEXT_LINE);
        sb.append(CSEQ);
        sb.append(seq);
        sb.append(NEXT_LINE);
        sb.append(SERVER);
        sb.append(NEXT_LINE);
//        sb.append("Cache-Control: no-cache\r\n");
        if (isNoData) {
            sb.append("Content-Length: 0\r\n");
        }
        return sb;
    }

    public static String responseGetParameter(String seq) {
        StringBuilder sb = createResponse(seq, false);
        sb.append(NEXT_LINE);
        return sb.toString();
    }


    public static String responseTeardown(String seq) {
        StringBuilder sb = createResponse(seq, false);
        sb.insert(0, "TEARDOWN \r\n");
        return sb.toString();
    }

    public static String responsePlay(String session, String seq, String rtpInfo) {
        StringBuilder sb = createResponse(seq, false);
        sb.append(rtpInfo);
        sb.append(NEXT_LINE);
//        sb.append("Range: npt=0.0-596.48\r\n");
        sb.append(SESSION);
        sb.append(session);
        sb.append(DEFAULT_TIMEOUT);
        sb.append("\r\n\r\n");
        return sb.toString();
    }

//    public static String responsePlay(String seq, String rtpInfo) {
//        StringBuilder sb = createResponse(seq);
//        sb.append("RTP-Info: ");
//        sb.append(rtpInfo);
//        sb.append("\r\n");
//        sb.append("Session: ");
//        sb.append(SESSION);
//        sb.append("\r\n");
//        return sb.toString();
//    }

    /**
     * SETUP 回应
     *
     * @param seq
     * @param transport
     * @return
     */
    public static String responseSetup(String session, String seq, String transport) {
        StringBuilder sb = createResponse(seq, true);
        sb.append("Transport: ");
        sb.append(transport);
        sb.append(NEXT_LINE);
        sb.append(SESSION);
        sb.append(session);
        sb.append(DEFAULT_TIMEOUT);
        sb.append(NEXT_LINE);
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    /**
     * OPTIONS 回应
     *
     * @param seq
     * @return
     */
    public static String responseOptions(String seq) {
        StringBuilder sb = createResponse(seq, true);
        sb.append("Public: DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS, ANNOUNCE, RECORD, GET_PARAMETER\r\n");
//        sb.append("Supported: play.basic, con.persistent\r\n");
        sb.append(NEXT_LINE);
        return sb.toString();
    }

    /**
     * Describe 回应
     *
     * @param session 会话id
     * @param address 推流端ip和端口
     * @param seq     当前会话次数（递增1）
     * @param data
     * @return
     */
    public static String responseDescribe(String session, String address, String seq, String data) {
        StringBuilder sb = createResponse(seq, false);
        sb.append("Content-Length: " + data.length());
        sb.append(NEXT_LINE);
        sb.append("Content-Base: flow.rtsp://" + address + "/\r\n");//推流端ip和端口
        sb.append("Content-Type: application/sdp\r\n");
        sb.append(SESSION);
        sb.append(session);
        sb.append(DEFAULT_TIMEOUT);
        sb.append(NEXT_LINE);
        sb.append(NEXT_LINE);
        sb.append(data);
        return sb.toString();
    }

    public static String responsePause(String seq) {
        return responseGetParameter(seq);
    }

    /**
     * 返回错误响应码
     *
     * @param status RtspProtocol.STATUS_BAD_REQUEST RtspProtocol.STATUS_NOT_FOUND RtspProtocol.STATUS_INTERNAL_SERVER_ERROR
     * @return
     */
    public static String responseError(String status, String seq) {
        StringBuilder sb = createResponse(seq, false);
        sb.replace(0, STATUS_OK.length(), status);
        return sb.toString();
    }
}
