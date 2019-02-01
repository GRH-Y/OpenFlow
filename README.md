# OpenFlow

这是纯java写的视频推流开源库，具有高扩张性，采用nio通信，目前支持rtsp协议推mp4视频文件或者摄像头（h264）,声音aac 适用于android和java环境

# 使用例子:
    String path = "i:\\viode\\孤独的生还者.mp4";
    NioServerFactory factory = NioServerFactory.getFactory();
    RtspServer rtspServer = new RtspServer(554);
    Mp4DateSrc dataSrc = new Mp4DateSrc(rtspServer, path);
    dataSrc.initVideoEncode();
    dataSrc.initAudioEncode();
    factory.open();
    factory.addTask(rtspServer);