package server.rtsp.joggle;

public interface IParameterConfig {

    String getBase64SPS();


    String getBase64PPS();


    String getProfileLevel();


    String getConfig();


    int getSamplingRate();
}
