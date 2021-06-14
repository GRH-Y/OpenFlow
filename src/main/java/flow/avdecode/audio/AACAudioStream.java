package flow.avdecode.audio;

import flow.avdecode.NalStream;
import flow.core.joggle.SteamType;
import flow.rtsp.packet.NalPacket;

public class AACAudioStream extends NalStream {


    /**
     * 音频帧率
     */
    private long audioClock = 8000;

    private long config = 1490;

    private String samplingRate = "12000/2";

    /**
     * 创建AudioStream
     */
    public AACAudioStream() {
        super(SteamType.AUDIO);
    }

    public long getClockFrequency() {
        return audioClock;
    }

    public long getConfig() {
        return config;
    }

    public void resetNalPacket(NalPacket packet) {

    }

    @Override
    protected boolean onSteamBeginInit() {
        return false;
    }

    @Override
    protected void onSteamExeNalData(boolean initResult) {

    }

    @Override
    protected void onSteamDestroy() {

    }

    public String getSamplingRate() {
        return samplingRate;
    }
}
