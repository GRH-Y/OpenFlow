package flow.rtsp;

import flow.avdecode.audio.DecodeMP4AACStream;
import flow.avdecode.video.DecodeMP4H264Stream;
import flow.core.FlowDataSrc;
import flow.core.FlowStream;
import util.StringEnvoy;

public class Mp4DateSrc extends FlowDataSrc {

    private String filePath;

    public Mp4DateSrc(String filePath) {
        if (StringEnvoy.isEmpty(filePath)) {
            throw new NullPointerException("filePath is null !!!!");
        }
        this.filePath = filePath;
        initSteam();
    }

    protected <T extends FlowStream> T initVideoSteam() {
        if (videoStream == null) {
            videoStream = new DecodeMP4H264Stream(filePath);

        }
        return (T) videoStream;
    }

    protected <T extends FlowStream> T initAudioSteam() {
        if (audioStream == null) {
            audioStream = new DecodeMP4AACStream(filePath);

        }
        return (T) audioStream;
    }

    @Override
    public void startStream() {
        if (videoStream != null) {
            videoStream.startSteam();
        }
        if (audioStream != null) {
//            audioStream.startSteam();
        }
    }

    @Override
    public void stopStream() {
        if (videoStream != null) {
            videoStream.stopSteam();
        }
        if (audioStream != null) {
//            audioStream.stopSteam();
        }
    }

    public boolean isStreamRun() {
        return videoStream.isStreamRun() || audioStream.isStreamRun();
    }

    @Override
    public void release() {
        stopStream();
        videoStream = null;
        audioStream = null;
    }
}
