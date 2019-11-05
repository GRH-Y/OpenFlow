package server.avdecode;

import log.LogDog;
import server.rtsp.packet.NalPacket;
import task.executor.BaseConsumerTask;
import task.executor.ConsumerQueueAttribute;
import task.executor.TaskContainer;
import task.executor.joggle.IConsumerAttribute;
import task.executor.joggle.IConsumerTaskExecutor;
import task.executor.joggle.ILoopTaskExecutor;
import task.executor.joggle.ITaskContainer;
import task.message.MessageEnvelope;
import task.message.joggle.IMsgPostOffice;

/**
 * nal流
 */
public abstract class NalSteam {

    protected ITaskContainer taskContainer;
    protected boolean steamTrigger = false;
    private String receiveMethodName = null;
    protected IMsgPostOffice msgPostOffice = null;

    public NalSteam() {
        SteamTask steamTask = new SteamTask();
        taskContainer = new TaskContainer(steamTask);
        taskContainer.getTaskExecutor().setAttribute(new ConsumerQueueAttribute<>());
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
        taskContainer.getTaskExecutor().idleStopTask();
    }

    //===================start 生命周期回调方法======================

    protected abstract boolean onSteamBeginInit();

    protected abstract void onSteamEndInit();

    protected abstract void onSteamCreateNalData();

    protected abstract void onSteamDestroy();

    //=====================end 生命周期回调方法====================

    private class SteamTask extends BaseConsumerTask {
        private boolean initResult = false;
        private ILoopTaskExecutor asyncExecutor = null;

        @Override
        protected void onInitTask() {
            initResult = onSteamBeginInit();
            if (initResult) {
                onSteamEndInit();
                //开启异步线程处理发送数据
                IConsumerTaskExecutor executor = taskContainer.getTaskExecutor();
                asyncExecutor = executor.startAsyncProcessData();
            } else {
                taskContainer.getTaskExecutor().stopTask();
            }
        }

        private void sendData(Object data) {
            if (data != null) {
                MessageEnvelope envelope = new MessageEnvelope();
                envelope.setData(data);
                envelope.setMethodName(receiveMethodName);
                msgPostOffice.sendEnvelope(envelope);
            }
        }

        @Override
        protected void onCreateData() {
            if (initResult) {
                onSteamCreateNalData();
            }
        }

        @Override
        protected void onProcess() {
            if (initResult) {
                IConsumerAttribute attribute = taskContainer.getTaskExecutor().getAttribute();
                Object data = attribute.popCacheData();
                if (data == null) {
                    asyncExecutor.pauseTask();
                }
                sendData(data);
            }
        }

        @Override
        protected void onIdleStop() {
            IConsumerAttribute attribute = taskContainer.getTaskExecutor().getAttribute();
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
