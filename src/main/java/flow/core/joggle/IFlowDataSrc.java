package flow.core.joggle;

import flow.core.FlowStream;

public interface IFlowDataSrc {

    <T extends FlowStream> T getVideoStream();

    <T extends FlowStream> T getAudioStream();

    void startStream();

    void stopStream();

    boolean isStreamRun();

    void release();

}
