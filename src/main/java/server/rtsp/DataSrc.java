package server.rtsp;


import server.avedcoder.audio.EncodeAACStream;
import server.avedcoder.video.DecodeMP4Stream;
import server.rtsp.packet.NalPacket;
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
        mp4Stream.setMsgPostOffice(postOffice,"onAudioVideo");
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

    private void sendData(NalPacket packet, List<RtpSocket> socketList) {
        RtpSocket socket = socketList.get(0);
        RtpPacket rtpPacket = socket.sendNalPacket(packet);
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
                sendData(packet,audioSet);
            } else {
                sendData(packet,videoSet);
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
