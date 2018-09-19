import connect.network.nio.NioServerFactory;
import server.rtsp.DataSrc;
import server.rtsp.RtspServer;
import util.Logcat;

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

        Logcat.d("==> test");
        NioServerFactory factory = NioServerFactory.getFactory();
        factory.open();
        RtspServer rtspServer = new RtspServer(3333);
        DataSrc dataSrc = new DataSrc();
        dataSrc.initVideoEncode();
        dataSrc.initAudioEncode();
        rtspServer.setDataSrc(dataSrc);
        factory.addTask(rtspServer);
    }
}
