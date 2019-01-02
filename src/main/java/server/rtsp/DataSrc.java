package server.rtsp;


import server.avedcoder.audio.EncodeAACStream;
import server.avedcoder.video.DecodeMP4Stream;
import server.rtsp.packet.NalPacket;
import server.rtsp.packet.RtcpPacket;
import server.rtsp.packet.RtpPacket;
import server.rtsp.rtp.RtpSocket;
import task.message.MessageCourier;
import task.message.MessageEnvelope;
import task.message.MessagePostOffice;
import util.LogDog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 9/11/2017.
 */

public class DataSrc {
    private DecodeMP4Stream mp4Stream = null;
    private EncodeAACStream audioThread = null;

    private List<RtpSocket> videoSet;
    private List<RtpSocket> audioSet;

    private RtpPacket videoRtpPacket;
    private RtpPacket audioRtpPacket;

    private RtcpPacket videoRtcpPacket;
    private RtcpPacket audioRtcpPacket;

    private MessagePostOffice postOffice;
    private MessageCourier courier;

    private String filePath;

    public DataSrc(String filePath) {
        this.filePath = filePath;
        if (filePath == null) {
            throw new NullPointerException("filePath is null !!!!");
        }
        courier = new MessageCourier(this);
        postOffice = new MessagePostOffice();
        courier.addEnvelopeServer(postOffice);
        videoSet = new ArrayList<>();
        audioSet = new ArrayList<>();
//        videoRtpPacket = new RtpPacket();
//        audioRtpPacket = new RtpPacket();
//        videoRtpPacket.setClockFrequency(90000);
//        audioRtpPacket.setClockFrequency(8000);
//        videoRtcpPacket = new RtcpPacket(videoRtpPacket.getSSRC());
//        audioRtcpPacket = new RtcpPacket(audioRtpPacket.getSSRC());
    }

    public void setAudioRtcpPacket(RtcpPacket audioRtcpPacket) {
        this.audioRtcpPacket = audioRtcpPacket;
    }

    public void setVideoRtcpPacket(RtcpPacket videoRtcpPacket) {
        this.videoRtcpPacket = videoRtcpPacket;
    }

    public void setAudioRtpPacket(RtpPacket audioRtpPacket) {
        this.audioRtpPacket = audioRtpPacket;
    }

    public void setVideoRtpPacket(RtpPacket videoRtpPacket) {
        this.videoRtpPacket = videoRtpPacket;
    }


    public RtpPacket getVideoRtpPacket() {
        return videoRtpPacket;
    }

    public RtpPacket getAudioRtpPacket() {
        return audioRtpPacket;
    }

    public RtcpPacket getVideoRtcpPacket() {
        return videoRtcpPacket;
    }

    public RtcpPacket getAudioRtcpPacket() {
        return audioRtcpPacket;
    }

    public void release() {
        postOffice.release();
        courier.release();
        try {
            finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void addAudioSocket(RtpSocket audio) {
        if (audioSet.size() == 0) {
            audioThread.setAudioOutSwitch(true);
        }
        audioSet.add(audio);
    }

    public void addVideoSocket(RtpSocket video) {
        if (videoSet.size() == 0) {
//            videoThread.setVideoOutSwitch(true);
            mp4Stream.setVideoOutSwitch(true);
            mp4Stream.resumeSteam();
        }
        videoSet.add(video);
    }

    public void removeAudioSocket(RtpSocket audio) {
        audioSet.remove(audio);
        if (audioSet.size() == 0) {
            audioThread.setAudioOutSwitch(false);
            stopAudioEncode();
        }
    }

    public void removeVideoSocket(RtpSocket video) {
        videoSet.remove(video);
        if (videoSet.size() == 0) {
//            videoThread.setVideoOutSwitch(false);
            mp4Stream.setVideoOutSwitch(false);
            stopVideoEncode();
        }
    }

    public void initVideoEncode() {
//        if (videoThread == null)
//        {
//            videoThread = new EncodeH264Stream(surfaceView);
//            videoThread.setVideoSize(1280, 720, 30);//1080x1920
//        }
        if (mp4Stream == null) {
            try {
                mp4Stream = new DecodeMP4Stream(filePath);
            } catch (Exception e) {
                e.printStackTrace();
                mp4Stream = null;
            }
        }
    }

    public void initAudioEncode() {
        audioThread = new EncodeAACStream(filePath);
    }

    public void startVideoEncode() {
//        videoThread.setMsgPostOffice(postOffice);
//        videoThread.startEncode();
        mp4Stream.setMsgPostOffice(postOffice, "onAudioVideo");
        mp4Stream.startSteam();
    }

    public void stopVideoEncode() {
//        videoThread.stopEncode();
        mp4Stream.stopSteam();
    }

    public void startAudioEncode() {
        audioThread.setMsgPostOffice(postOffice);
        audioThread.startEncode();
    }

    public void stopAudioEncode() {
        audioThread.stopEncode();
    }

    public String getBase64SPS() {
//        return videoThread.getBase64SPS();
        return mp4Stream.getBase64SPS();
    }

    public String getBase64PPS() {
//        return videoThread.getBase64PPS();
        return mp4Stream.getBase64PPS();
    }

    public String getProfileLevel() {
//        return videoThread.getProfileLevel();
        return mp4Stream.getProfileLevel();
    }

    public String getConfig() {
        return Integer.toHexString(audioThread.getConfig());
    }

    public int getSamplingRate() {
        return audioThread.getSamplingRate();
    }


    private void sendData(RtpPacket rtpPacket, List<RtpSocket> socketList) {
        RtpSocket socket = socketList.get(0);
        socket.sendRtpPacket(rtpPacket);
        for (int index = 1; index < socketList.size(); index++) {
            RtpSocket tmp = socketList.get(index);
            tmp.sendRtpPacket(rtpPacket);
        }
    }


    private void onAudioVideo(MessageEnvelope envelope) {
        if (audioSet.size() == 0 || videoSet.size() == 0) {
            return;
        }
        Object object = envelope.getData();
        if (object instanceof NalPacket) {
            NalPacket packet = (NalPacket) envelope.getData();
            if (NalPacket.PacketType.AUDIO == packet.getPacketType()) {
                audioRtpPacket.setNalPacket(packet);
                sendData(audioRtpPacket, audioSet);
            } else {
                videoRtpPacket.setNalPacket(packet);
                sendData(videoRtpPacket, videoSet);
            }
            packet.setFullNal(false);
        } else if (object instanceof RtpPacket) {
            RtpPacket packet = (RtpPacket) envelope.getData();
            if (NalPacket.PacketType.AUDIO == packet.getPacketType()) {
                for (RtpSocket audio : audioSet) {
                    audio.sendRtpPacket(packet);
                }
            } else {
                for (RtpSocket video : videoSet) {
                    video.sendRtpPacket(packet);
                }
            }
        } else {
            LogDog.e("==> DataSrc onAudioVideo other data !!!");
        }
    }
}
