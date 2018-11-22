import connect.network.nio.NioClientFactory;
import connect.network.nio.NioServerFactory;
import server.p2p.P2PSClient;
import server.p2p.P2PServer;
import util.NetUtils;

public class OpenFlowTestMain {

    public static void main(String[] args) {

//        String path = "/home/dev-ubuntu/qq_video.mp4";
//        Mp4Analysis mp4Analysis = new Mp4Analysis();
//        try {
//            mp4Analysis.init(path);
//            mp4Analysis.findVideoBox();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        NioServerFactory factory = NioServerFactory.getFactory();
        factory.open();
//        RtspServer rtspServer = new RtspServer(3333);
//        DataSrc dataSrc = new DataSrc();
//        dataSrc.initVideoEncode();
//        dataSrc.initAudioEncode();
//        rtspServer.setDataSrc(dataSrc);
//        factory.addTask(rtspServer);

        String p2pIp = NetUtils.getLocalIp("wlan");
        int p2pPort = 4444;
        P2PServer p2PServer = new P2PServer(p2pIp, p2pPort);
        factory.addTask(p2PServer);
        NioClientFactory clientFactory = NioClientFactory.getFactory();
        clientFactory.open();
        P2PSClient p2PSClient = new P2PSClient(p2pIp, p2pPort);
        clientFactory.addTask(p2PSClient);

    }
}
