package server.rtsp.joggle;

import server.rtsp.rtp.RtpSocket;

public interface IDataSrc extends IParameterConfig {

    void setSteamOutSwitch(boolean isOpen);

    void addAudioSocket(RtpSocket audio);

    void addVideoSocket(RtpSocket video);

    void removeAudioSocket(RtpSocket audio);

    void removeVideoSocket(RtpSocket video);


    <T> T initVideoEncode();

    <T> T initAudioEncode();

    void startVideoEncode();

    void startAudioEncode();

    void stopVideoEncode();

    void stopAudioEncode();

    void release();

}
