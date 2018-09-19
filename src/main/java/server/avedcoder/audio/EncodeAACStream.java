package server.avedcoder.audio;


import server.avedcoder.packet.NalPacket;
import task.executor.BaseConsumerTask;
import task.message.MessagePostOffice;

public class EncodeAACStream {
    private int encodingBitRate = 32000;
    private EncodeTask encodeTask = null;
    private boolean trigger = false;
    private String path = "F:\\FFOutput\\test_file_stream.mp4";

    public EncodeAACStream(String path) {
        this.path = path;
//        encodeTask = new EncodeTask();
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

    private class EncodeTask extends BaseConsumerTask<NalPacket> {
    }


}
