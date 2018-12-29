package server.avedcoder.audio;


import server.rtsp.packet.NalPacket;
import task.executor.BaseConsumerTask;
import task.message.MessagePostOffice;

public class EncodeAACStream {
    private int encodingBitRate = 32000;
    private DecodeTask encodeTask = null;
    private boolean trigger = false;
    private String path;

    public EncodeAACStream(String path) {
        this.path = path;
//        encodeTask = new DecodeTask();
    }

    public void setAudioOutSwitch(boolean audioOutSwitch) {
        this.trigger = audioOutSwitch;
    }

    public void stopEncode() {
//        encodeTask.stop();
    }

    public void startEncode() {
//        encodeTask.start();
    }

    public int getConfig() {
        return 5512;
    }

    public int getSamplingRate() {
        return encodingBitRate;
    }

    public void setMsgPostOffice(MessagePostOffice postOffice) {
//        encodeTask.setMsgPostOffice(postOffice);
    }

    private class DecodeTask extends BaseConsumerTask<NalPacket> {
    }


}
