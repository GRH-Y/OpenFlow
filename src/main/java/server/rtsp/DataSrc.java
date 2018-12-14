package server.rtsp;


import server.avedcoder.audio.EncodeAACStream;
import server.avedcoder.packet.NalPacket;
import server.avedcoder.packet.RtpPacket;
import server.avedcoder.video.EncodeMP4Stream;
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
    private EncodeMP4Stream mp4Stream = null;
    private EncodeAACStream audioThread = null;

    private List<RtpSocket> videoSet;
    private List<RtpSocket> audioSet;

    private MessagePostOffice postOffice;
    private MessageCourier courier;

    public DataSrc() {
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
            mp4Stream.pushStream();
        }
        videoSet.add(video);
    }

    public void removeAudioSocket(RtpSocket audio) {
        audioSet.remove(audio);
        if (audioSet.size() == 0) {
            audioThread.setAudioOutSwitch(false);
        }
    }

    public void removeVideoSocket(RtpSocket video) {
        videoSet.remove(video);
        if (videoSet.size() == 0) {
//            videoThread.setVideoOutSwitch(false);
            mp4Stream.setVideoOutSwitch(false);
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
                String path = "/home/dev-ubuntu/Videos/test.mp4";
                mp4Stream = new EncodeMP4Stream(path);
            } catch (Exception e) {
                e.printStackTrace();
                mp4Stream = null;
            }
        }
    }

    public void initAudioEncode() {
        String path = "/home/dev-ubuntu/test.mp4";
        audioThread = new EncodeAACStream(path);
    }

    public void startVideoEncode() {
//        videoThread.setMsgPostOffice(postOffice);
//        videoThread.startEncode();
        mp4Stream.setMsgPostOffice(postOffice);
        mp4Stream.openStream();
    }

    public void stopVideoEncode() {
//        videoThread.stopEncode();
        mp4Stream.closeStream();
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


    private void onAudioVideo(MessageEnvelope envelope) {
        if (audioSet.size() == 0 || videoSet.size() == 0) {
            return;
        }
        Object object = envelope.getData();
        if (object instanceof NalPacket) {
            NalPacket packet = (NalPacket) envelope.getData();
            if (NalPacket.PacketType.AUDIO == packet.getPacketType()) {
                RtpSocket tmp = audioSet.get(0);
                RtpPacket rtpPacket = tmp.sendNalPacket(packet);
                for (int index = 1; index < audioSet.size(); index++) {
                    RtpSocket audio = audioSet.get(index);
                    audio.sendRtpPacket(rtpPacket);
                }
            } else {
                RtpSocket tmp = videoSet.get(0);
                RtpPacket rtpPacket = tmp.sendNalPacket(packet);
                for (int index = 1; index < videoSet.size(); index++) {
                    RtpSocket video = videoSet.get(index);
                    video.sendRtpPacket(rtpPacket);
                }
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
