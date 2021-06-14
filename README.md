# OpenFlow

    这是纯java写的视频推流开源库，能在任何平台下运行，具有高扩张性，采用nio通信
    目前支持rtsp协议推mp4视频文件或者摄像头（h264）,声音aac(暂未实现) 
    适用于android和java环境

# 使用例子:

    //一句代码就能实现rtsp服务
    RtspOpenFlow.getInstance().startFlow();

# 说明
    OpenFlow类有下面3个方法需要重写，能容易扩展实现不用的流服务（比如后续可以实现rtmp服务）
    //流服务
    initFlowServer()
    //数据源
    initFlowDataSrc()
    //推流
    initFlowPushStream()

# 实现Rtsp例子

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