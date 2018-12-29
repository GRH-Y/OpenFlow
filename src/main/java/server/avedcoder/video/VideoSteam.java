package server.avedcoder.video;

import server.avedcoder.mp4.Mp4Analysis;
import server.rtsp.packet.ContentPacket;
import server.rtsp.packet.NalPacket;
import server.rtsp.packet.RtpPacket;
import task.executor.BaseConsumerTask;
import task.executor.ConsumerQueueAttribute;
import task.executor.TaskContainer;
import task.executor.joggle.IConsumerAttribute;
import task.executor.joggle.IConsumerTaskExecutor;
import task.message.MessageEnvelope;
import task.message.joggle.IMsgPostOffice;
import util.LogDog;
import util.MultiplexCache;

public abstract class VideoSteam {

    protected IMsgPostOffice msgPostOffice;
    protected TaskContainer taskContainer;
    protected boolean steamTrigger = false;
    private String receiveMethodName;

    protected MultiplexCache<ContentPacket> contentPacketCache = null;
    protected byte[] sps = null;
    protected byte[] pps = null;

    public VideoSteam() {
        NalSteam steam = new NalSteam();
        taskContainer = new TaskContainer(steam);
        taskContainer.setAttribute(new ConsumerQueueAttribute<>());
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

    public void setVideoOutSwitch(boolean trigger) {
        this.steamTrigger = trigger;
    }

    public void setMsgPostOffice(IMsgPostOffice postOffice, String receiveMethodName) {
        this.msgPostOffice = postOffice;
        this.receiveMethodName = receiveMethodName;
    }


    public String getBase64SPS() {
//        return "Z0KAH9oBQBboBtChNQ==";
        return Mp4Analysis.getBase64SPS(sps);
    }

    public String getBase64PPS() {
//        return "aM4G4g==";
        return Mp4Analysis.getBase64PPS(pps);
    }

    public String getProfileLevel() {
//        return "42801f";
        return Mp4Analysis.getProfileLevel(sps);
    }

    protected abstract boolean onBeginInit();

    protected abstract void onEndInit();

    protected abstract void onCreateNalData();

    protected abstract void onDestroy();

    private class NalSteam extends BaseConsumerTask<NalPacket> {

        private void initCache() {
            int size = 100;
            contentPacketCache = new MultiplexCache<>(size);
            for (int i = 0; i < size; i++) {
                ContentPacket packet = new ContentPacket(RtpPacket.MTU);
                contentPacketCache.setRepeatData(packet);
            }
        }

        @Override
        protected void onInitTask() {
            boolean ret = onBeginInit();
            if (ret) {
                initCache();
                //开启异步线程处理发送数据
                IConsumerTaskExecutor executor = taskContainer.getTaskExecutor();
                executor.startAsyncProcessData();
                onEndInit();
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
            onCreateNalData();
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
            LogDog.w("==>NalSteam onIdleStop cache size = " + attribute.getCacheDataSize());
            Object data = attribute.popCacheData();
            while (data != null) {
                sendData(data);
                data = attribute.popCacheData();
            }
            LogDog.w("==>NalSteam onIdleStop end !!!");
        }

        @Override
        protected void onDestroyTask() {
            onDestroy();
            if (contentPacketCache != null) {
                contentPacketCache.release();
                contentPacketCache = null;
            }
        }
    }
}
