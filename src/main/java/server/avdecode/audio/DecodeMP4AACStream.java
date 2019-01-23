package server.avdecode.audio;


public class DecodeMP4AACStream extends AudioSteam {
    private int encodingBitRate = 32000;
    private String path;

    public DecodeMP4AACStream(String path) {
        this.path = path;
    }

    public int getConfig() {
        return 5512;
    }

    public int getSamplingRate() {
        return encodingBitRate;
    }

}
