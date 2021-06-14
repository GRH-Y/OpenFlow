package flow.rtsp;

import flow.core.FlowDataSrc;
import flow.core.FlowPushStream;
import flow.core.FlowServer;
import flow.core.OpenFlow;
import util.AnalysisConfig;
import util.NetUtils;

public class RtspOpenFlow extends OpenFlow {

    private volatile static RtspOpenFlow OPEN_FLOW;

    private static final String KEY_VIDEO_FILE_PATH = "videoFilePath";

    public static RtspOpenFlow getInstance() {
        if (OPEN_FLOW == null) {
            synchronized (RtspOpenFlow.class) {
                if (OPEN_FLOW == null) {
                    OPEN_FLOW = new RtspOpenFlow();
                }
            }
        }
        return OPEN_FLOW;
    }


    public static void release() {
        if (OPEN_FLOW != null) {
            return;
        }
        OPEN_FLOW.stopFlow();
        OPEN_FLOW = null;
    }


    /**
     * 配置服务
     * @return
     */
    @Override
    protected FlowServer initFlowServer() {
        String ip = NetUtils.getLocalIp("wlan0");
        RtspServer rtspServer = new RtspServer();
        rtspServer.setAddress(ip, 554);
        return rtspServer;
    }

    /**
     * 配置数据源
     * @return
     */
    @Override
    protected FlowDataSrc initFlowDataSrc() {
        return new Mp4DateSrc(AnalysisConfig.getInstance().getValue(KEY_VIDEO_FILE_PATH));
    }

    /**
     * 配置推流管道
     * @return
     */
    @Override
    protected FlowPushStream initFlowPushStream() {
        return new RtspPushStream(this);
    }
}
