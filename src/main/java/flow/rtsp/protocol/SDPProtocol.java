package flow.rtsp.protocol;

public class SDPProtocol {
    //v=<version> (协议版本)
    //o=<username> <session id> <version> <network type> <address type> <address> (所有者/创建者和会话标识符)
    //s=<session name>    (会话名称)
    //i=<session description> (会话信息)
    //u=<URI> (URI 描述)
    //e=<email address> (Email 地址)
    //p=<phone number> (电话号码)
    //c=<network type> <address type> <connection address> (连接信息)
    //b=<modifier>:<bandwidth-value> (带宽信息)
    //t=<start time> <stop time> (会话活动时间)
    //r=<repeat interval> <active duration> <list of offsets from start-time>(0或多次重复次数)
    //z=<adjustment time> <offset> <adjustment time> <offset> ....
    //k=<method>
    //k=<method>:<encryption key> (加密密钥)
    //a=<attribute> (0 个或多个会话属性行)
    //a=<attribute>:<value>
    //m=<media> <port> <transport> <fmt list> (媒体名称和传输地址)

    private StringBuilder sdpBuilder;

    private String session = null;
    private String version = null;
    private static final String net_adr_type = "IN IP4";
    private static final String endTag = "\r\n";

    public SDPProtocol() {
        sdpBuilder = new StringBuilder();
        initSession();
        sdpBuilder.append("v=0");
        sdpBuilder.append(endTag);

    }

    private void initSession() {
        long time = System.currentTimeMillis() / 1000;
        session = String.valueOf(time);
        version = String.valueOf(time + 1);
    }

    public void initParameter() {
//        sdpBuilder.append("o=- " + session + " " + version + " " + net_adr_type + " " + flowNetFactory.getRtspSerArs() + "\r\n");
    }

    public String builder() {
        return sdpBuilder.toString();
    }
}
