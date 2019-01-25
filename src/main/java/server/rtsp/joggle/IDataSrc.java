package server.rtsp.joggle;

import server.rtsp.rtp.RtpSocket;

public interface IDataSrc {

    void addAudioSocket(RtpSocket audio);

    void addVideoSocket(RtpSocket video);


    void removeAudioSocket(RtpSocket audio);


    void removeVideoSocket(RtpSocket video);


    void initVideoEncode();

    void initAudioEncode();

    void startVideoEncode();

    void startAudioEncode();


    void stopVideoEncode();


    void stopAudioEncode();


    void release();

}
