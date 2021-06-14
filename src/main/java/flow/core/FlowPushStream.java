package flow.core;

import flow.core.joggle.IFlowPushStream;
import flow.core.joggle.IFlowSteamDataPushListener;

import java.util.LinkedList;
import java.util.List;

public abstract class FlowPushStream<E, T> implements IFlowPushStream<E>, IFlowSteamDataPushListener<T> {

    protected List<E> videoClientList;
    protected List<E> audioClientList;

    protected OpenFlow openFlow;


    public FlowPushStream(OpenFlow openFlow) {
        this.openFlow = openFlow;

        videoClientList = new LinkedList<>();
        audioClientList = new LinkedList<>();

        FlowDataSrc dataSrc = openFlow.getFlowDataSrc();
        FlowStream videoStream = dataSrc.getVideoStream();
        if (videoStream != null) {
            videoStream.setStreamDataPushListener(this);
        }
        FlowStream audioStream = dataSrc.getAudioStream();
        if (audioStream != null) {
            audioStream.setStreamDataPushListener(this);
        }
    }


    @Override
    public void addVideoClient(E client) {
        synchronized (videoClientList) {
            videoClientList.add(client);
        }
    }

    @Override
    public void addAudioClient(E client) {
        synchronized (audioClientList) {
            audioClientList.add(client);
        }
    }

    @Override
    public void removeVideoClient(E client) {
        synchronized (videoClientList) {
            videoClientList.remove(client);
        }
    }

    @Override
    public void removeAudioClient(E client) {
        synchronized (audioClientList) {
            audioClientList.remove(client);
        }
    }

    @Override
    public void release() {
        videoClientList.clear();
        audioClientList.clear();
        openFlow = null;
    }

}
