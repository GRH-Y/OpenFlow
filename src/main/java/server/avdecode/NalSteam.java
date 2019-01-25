package server.avdecode;

import server.rtsp.packet.NalPacket;
import task.executor.BaseConsumerTask;
import task.executor.ConsumerQueueAttribute;
import task.executor.TaskContainer;
import task.executor.joggle.IConsumerAttribute;
import task.executor.joggle.IConsumerTaskExecutor;
import task.message.MessageEnvelope;
import task.message.joggle.IMsgPostOffice;
import util.LogDog;

/**
 * nal流
 */
public abstract class NalSteam {

    protected TaskContainer taskContainer;
    protected boolean steamTrigger = false;
    private String receiveMethodName = null;
    protected IMsgPostOffice msgPostOffice = null;

    public NalSteam() {
        SteamTask steamTask = new SteamTask();
        taskContainer = new TaskContainer(steamTask);
        taskContainer.setAttribute(new ConsumerQueueAttribute<>());
    }

    public void setSteamOutSwitch(boolean trigger) {
        this.steamTrigger = trigger;
    }

    public void setMsgPostOffice(IMsgPostOffice postOffice, String receiveMethodName) {
        this.msgPostOffice = postOffice;
        this.receiveMethodName = receiveMethodName;
    }

    public void pauseSteam() {
        taskContainer.getTaskExecutor().pauseTask();
    }

    public void resumeSteam() {
        taskContainer.getTaskExecutor().resumeTask();
    }

    public void startSteam() {
        taskContainer.getTaskExecutor().startTask();
    }

    public void stopSteam() {
        taskContainer.getTaskExecutor().stopTask();
    }

    //===================start 生命周期回调方法======================

    protected abstract boolean onSteamBeginInit();

    protected abstract void onSteamEndInit();

    protected abstract void onSteamCreateNalData();

    protected abstract void onSteamDestroy();

    //=====================end 生命周期回调方法====================

    private class SteamTask extends BaseConsumerTask<NalPacket> {

        @Override
        protected void onInitTask() {
            boolean ret = onSteamBeginInit();
            if (ret) {
                onSteamEndInit();
                //开启异步线程处理发送数据
                IConsumerTaskExecutor executor = taskContainer.getTaskExecutor();
                executor.startAsyncProcessData();
            }
        }

        private void sendData(Object data) {
            MessageEnvelope envelope = new MessageEnvelope();
            envelope.setData(data);
            envelope.setMethodName(receiveMethodName);
            msgPostOffice.sendEnvelope(envelope);
        }

        @Override
        protected void onCreateData() {
            onSteamCreateNalData();
        }

        @Override
        protected void onProcess() {
            IConsumerAttribute attribute = taskContainer.getAttribute();
            Object data = attribute.popCacheData();
            sendData(data);
        }

        @Override
        protected void onIdleStop() {
            IConsumerAttribute attribute = taskContainer.getAttribute();
            LogDog.w("==>SteamTask onIdleStop cache size = " + attribute.getCacheDataSize());
            Object data = attribute.popCacheData();
            while (data != null) {
                sendData(data);
                data = attribute.popCacheData();
            }
            LogDog.w("==>SteamTask onIdleStop end !!!");
        }

        @Override
        protected void onDestroyTask() {
            onSteamDestroy();

        }
    }
}
