package flow.core;

import flow.core.joggle.IFlowSteamDataPushListener;
import flow.core.joggle.ISteamStateListener;
import flow.core.joggle.IStream;
import flow.core.joggle.SteamType;
import task.executor.BaseLoopTask;
import task.executor.TaskContainer;
import task.executor.joggle.ITaskContainer;

/**
 * nal流
 */
public abstract class FlowStream<T> implements IStream {

    protected IFlowSteamDataPushListener<T> pushListener;
    protected ISteamStateListener stateListener;
    protected ITaskContainer taskContainer;
    protected SteamType steamType;

    /**
     * 创建NalSteam
     */
    public FlowStream(SteamType type) {
        this.steamType = type;
        SteamTask steamTask = new SteamTask();
        taskContainer = new TaskContainer(steamTask);
    }


    /**
     * 设置nal数据输出监听
     *
     * @param listener
     */
    public void setStreamDataPushListener(IFlowSteamDataPushListener listener) {
        this.pushListener = listener;
    }

    /**
     * 设置steam 流程监听
     *
     * @param listener
     */
    public void setSteamStateListener(ISteamStateListener listener) {
        this.stateListener = listener;
    }


    public boolean isStreamRun() {
        return taskContainer.getTaskExecutor().isAliveState();
    }

    @Override
    public void pauseSteam() {
        taskContainer.getTaskExecutor().pauseTask();
    }

    @Override
    public void resumeSteam() {
        taskContainer.getTaskExecutor().resumeTask();
    }

    @Override
    public void startSteam() {
        taskContainer.getTaskExecutor().startTask();
    }

    @Override
    public void stopSteam() {
        taskContainer.getTaskExecutor().stopTask();
    }

    //===================start 生命周期回调方法======================

    protected abstract boolean onSteamBeginInit();

    protected abstract void onSteamExeNalData(boolean initResult);

    protected abstract void onSteamDestroy();

    //=====================end 生命周期回调方法====================

    private class SteamTask extends BaseLoopTask {
        private boolean initResult = false;

        @Override
        protected void onInitTask() {
            initResult = onSteamBeginInit();
            if (stateListener != null) {
                stateListener.onStart(steamType);
            }
        }

        @Override
        protected void onRunLoopTask() {
            onSteamExeNalData(initResult);
            stopSteam();
        }

        @Override
        protected void onDestroyTask() {
            onSteamDestroy();
            if (stateListener != null) {
                stateListener.onStop(steamType);
            }
        }
    }
}
