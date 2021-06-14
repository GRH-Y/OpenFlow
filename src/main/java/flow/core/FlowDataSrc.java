package flow.core;


import flow.core.joggle.IFlowDataSrc;

/**
 * 数据源
 * Created by dell on 9/11/2017.
 */

public abstract class FlowDataSrc implements IFlowDataSrc {

    protected FlowStream videoStream;
    protected FlowStream audioStream;

    public void initSteam() {
        videoStream = initVideoSteam();
        audioStream = initAudioSteam();
    }

    abstract protected <T extends FlowStream> T initVideoSteam();

    abstract protected <T extends FlowStream> T initAudioSteam();

    @Override
    public <T extends FlowStream> T getVideoStream() {
        return (T) videoStream;
    }

    @Override
    public <T extends FlowStream> T getAudioStream() {
        return (T) audioStream;
    }

    @Override
    public boolean isStreamRun() {
        return false;
    }
}
