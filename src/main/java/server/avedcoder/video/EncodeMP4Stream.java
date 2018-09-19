package server.avedcoder.video;


import server.avedcoder.packet.ContentPacket;
import server.avedcoder.packet.NalPacket;
import server.avedcoder.packet.ParameterSetPacket;
import server.avedcoder.packet.RtpPacket;
import task.executor.BaseConsumerTask;
import task.executor.ConsumerQueueAttribute;
import task.executor.TaskContainer;
import task.executor.interfaces.IConsumerAttribute;
import task.executor.interfaces.IConsumerTaskExecutor;
import task.executor.interfaces.ILoopTaskExecutor;
import task.message.MessageCourier;
import task.message.MessageEnvelope;
import task.message.interfaces.IMsgPostOffice;
import util.IoUtils;
import util.Logcat;
import util.MultiplexCache;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.List;

public class EncodeMP4Stream extends BaseConsumerTask<NalPacket> {

    private TaskContainer taskContainer;
    private MessageCourier messageCourier;
    private String path;
    private RandomAccessFile file = null;
    private int[] arrayStco = null;
    private boolean trigger = false;
    private byte[] sps = null;
    private byte[] pps = null;
    private List<Mp4Analysis.Stsc> stscList = null;
    private volatile ParameterSetPacket parameterSetPacket = null;
    private volatile MultiplexCache<ContentPacket> contentPacketCache = null;

    private long oldTime = 0, duration = 0, ts = 0;
    private byte[] header = new byte[5];
    private int type, maxLength = RtpPacket.MTU - RtpPacket.RTP_HEADER_LENGTH - 2;

    public EncodeMP4Stream(String path) {
        this.path = path;
        messageCourier = new MessageCourier(this);
        taskContainer = new TaskContainer(this);
        taskContainer.setAttribute(new ConsumerQueueAttribute<>());
    }

    public void setVideoOutSwitch(boolean trigger) {
        this.trigger = trigger;
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

    public void setMsgPostOffice(IMsgPostOffice postOffice) {
        messageCourier.setEnvelopeServer(postOffice);
    }

    private void initCache() throws Exception {
        int size = 300;
        parameterSetPacket = new ParameterSetPacket(sps, pps, RtpPacket.RTP_HEADER_LENGTH);
        contentPacketCache = new MultiplexCache<>(size);
        for (int i = 0; i < size; i++) {
            Constructor constructor = ContentPacket.class.getConstructor(int.class);
            ContentPacket packet = (ContentPacket) constructor.newInstance(RtpPacket.MTU);
            contentPacketCache.setRepeatData(packet);
        }
    }

    private void initFile() throws Exception {
        Mp4Analysis mp4Analysis = new Mp4Analysis();
        mp4Analysis.init(path);
        mp4Analysis.findVideoBox();
        arrayStco = mp4Analysis.getStco();
        pps = mp4Analysis.getPPS();
        sps = mp4Analysis.getSPS();
        stscList = mp4Analysis.getListStsc();
        mp4Analysis.release();
        file = new RandomAccessFile(path, "r");
    }

    @Override
    protected void onInitTask() {
        Logcat.w("==>EncodeMP4Stream startTask ing ....");
        try {
            initFile();
            initCache();
            IConsumerTaskExecutor executor = taskContainer.getTaskExecutor();
            executor.startAsyncProcessData();
            if (!trigger) {
                Logcat.d("==>EncodeMP4Stream 等待 RTSP播放指令!");
                taskContainer.getTaskExecutor().waitTask(0);
                Logcat.d("==>EncodeMP4Stream 收到 RTSP播放指令!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreateData() {
        try {
            for (int frameIndex = 0; frameIndex < arrayStco.length; frameIndex++) {
                Mp4Analysis.Stsc stscObj = null;
                file.seek(0);//恢复文件指针到0位置
                int stco = arrayStco[frameIndex];
                IoUtils.skip(file, stco);//定位到要读取的位置
                //查找是否有复用的帧
                for (Mp4Analysis.Stsc tmp : stscList) {
                    if (tmp.firstChunk == frameIndex) {
                        stscObj = tmp;
                        break;
                    }
                }
                if (stscObj != null) {
                    //如果有复用帧
//                    int maxChunk =stscObj.SamplesPerChunk;
                    int maxChunk = stscObj.firstChunk == 0 ? stscObj.SamplesPerChunk + 1 : stscObj.SamplesPerChunk;
                    for (int index = 0; index < maxChunk; index++) {
                        if (!extractData()) {
                            return;
                        }
                    }
                } else {
                    //没有复用帧
//                    int length;
                    for (int index = 0; index < 12; index++) {
                        if (!extractData()) {
                            return;
                        }
                    }
                }
                Logcat.i("======================================================");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        taskContainer.getTaskExecutor().idleStopTask();
    }

    private void resumeTask() {
        IConsumerTaskExecutor executor = taskContainer.getTaskExecutor();
        ILoopTaskExecutor asyncExecutor = executor.getAsyncTaskExecutor();
        if (asyncExecutor != null) {
            asyncExecutor.resumeTask();
        } else {
            taskContainer.getTaskExecutor().resumeTask();
        }
    }

    /**
     * 提取数据
     */
    private boolean extractData() {
        IConsumerAttribute attribute = taskContainer.getAttribute();
        oldTime = System.nanoTime();
        IoUtils.readToFull(file, header);
        taskContainer.getTaskExecutor().sleepTask(33);
        ts += duration;
        int nalLength = Mp4Analysis.byteToInt(header, 0);
        type = header[4] & 0x1F;
        Logcat.v("==>EncodeMP4Stream type = " + type + " length = " + (nalLength + 4));

        //关键帧
        if (type == 5) {
            //发送sps和pps
            parameterSetPacket.setTime(ts);
            parameterSetPacket.setFullNal(true);
            if (trigger) {
                attribute.pushToCache(parameterSetPacket);
                resumeTask();
            }
        }

        //type = 1(非关键帧)
        if (nalLength <= maxLength) {
            ContentPacket contentPacket = contentPacketCache.getRepeatData();
            contentPacket.setTime(ts);
            byte[] data = contentPacket.getData();
            // Small NAL unit => Single NAL unit
            data[RtpPacket.RTP_HEADER_LENGTH] = header[4];
            nalLength--;
            int ret = IoUtils.readToFull(file, data, RtpPacket.RTP_HEADER_LENGTH + 1, nalLength);
            contentPacket.setFullNal(true);
            if (ret == IoUtils.FAIL) {
                closeStream();
                return false;
            }
            contentPacket.setLimit(nalLength + RtpPacket.RTP_HEADER_LENGTH);
            if (trigger) {
                attribute.pushToCache(contentPacket);
                resumeTask();
                contentPacketCache.setRepeatData(contentPacket);
            }
        } else {
            // Large NAL unit => Split nal unit
            // Set FU-A header
            header[1] = (byte) (header[4] & 0x1F);  // FU header type
            header[1] += 0x80; // Start bit
            // Set FU-A indicator
            header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
            header[0] += 28;

            int sum = 1;
            while (sum < nalLength && taskContainer.getTaskExecutor().getLoopState()) {
                ContentPacket contentPacket = contentPacketCache.getRepeatData();
                if (contentPacket == null) {
                    return false;
                }
                byte[] data = contentPacket.getData();

                data[RtpPacket.RTP_HEADER_LENGTH] = header[0];
                data[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];

                int len = nalLength - sum > maxLength ? maxLength : nalLength - sum;
                int ret = IoUtils.readToFull(file, data, RtpPacket.RTP_HEADER_LENGTH + 2, len);
                if (ret == IoUtils.FAIL) {
                    closeStream();
                    return false;
                }
                sum += len;
                // Last parameterSetPacket before next NAL
                if (sum >= nalLength) {
                    // End bit on
                    contentPacket.setFullNal(true);
                    data[RtpPacket.RTP_HEADER_LENGTH + 1] += 0x40;
                }
                contentPacket.setLimit(RtpPacket.RTP_HEADER_LENGTH + 2 + len);
                contentPacket.setTime(ts);
                if (trigger) {
                    attribute.pushToCache(contentPacket);
                    resumeTask();
                    contentPacketCache.setRepeatData(contentPacket);
                }
                // Switch startTask bit
                header[1] = (byte) (header[1] & 0x7F);
            }
        }
        duration = System.nanoTime() - oldTime;
        return true;
    }

    @Override
    protected void onProcess() {
        IConsumerAttribute attribute = taskContainer.getAttribute();
        Object data = attribute.popCacheData();
        MessageEnvelope envelope = new MessageEnvelope();
        envelope.setData(data);
        envelope.setMethodName("onAudioVideo");
        messageCourier.sendEnvelopSelf(envelope);
    }

    @Override
    protected void onIdleStop() {
        IConsumerAttribute attribute = taskContainer.getAttribute();
        Logcat.w("==>EncodeMP4Stream onIdleStop ing ..start..cache size = " + attribute.getCacheDataSize());
        Object data = attribute.popCacheData();
        while (data != null) {
            MessageEnvelope envelope = new MessageEnvelope();
            envelope.setData(data);
            envelope.setMethodName("onAudioVideo");
            messageCourier.sendEnvelopSelf(envelope);
            data = attribute.popCacheData();
        }
        Logcat.w("==>EncodeMP4Stream onIdleStop ing ..end..");
    }

    @Override
    protected void onDestroyTask() {
        Logcat.w("==>EncodeMP4Stream stopTask ing ....");
        try {
            file.close();
            file = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (contentPacketCache != null) {
            contentPacketCache.release();
            contentPacketCache = null;
        }
        if (parameterSetPacket != null) {
            parameterSetPacket.release();
            parameterSetPacket = null;
        }
    }

    public void pushStream() {
        taskContainer.getTaskExecutor().resumeTask();
    }

    public void openStream() {
        taskContainer.getTaskExecutor().startTask();
    }

    public void closeStream() {
        taskContainer.getTaskExecutor().stopTask();
    }
}
