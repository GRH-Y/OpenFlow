package server.avdecode.video;


import server.avdecode.mp4.Mp4Analysis;
import server.avdecode.mp4.NalAnalysis;
import server.camera.H264Preview;
import server.rtsp.packet.ContentPacket;
import server.rtsp.packet.NalPacket;
import server.rtsp.packet.RtpPacket;
import task.executor.TaskContainer;
import task.executor.joggle.IConsumerAttribute;
import task.executor.joggle.IConsumerTaskExecutor;
import task.executor.joggle.ILoopTaskExecutor;
import task.message.joggle.IMsgPostOffice;
import util.LogDog;

/**
 * 编码线程（把h264封装成RTP包）
 * Created by dell on 9/7/2017.
 */

public class DVRH264Stream extends VideoSteam {
    private IMsgPostOffice msgPostOffice;
    private TaskContainer container;
    private int width = 640;
    private int height = 480;
    private int pixelFormat = H264Preview.CAMERA_PIX_FMT_H264;
    private byte[] header = new byte[5];
    private long oldTime = 0, duration = 0, ts = 0;
    private byte[] sps = null;
    private byte[] pps = null;
    private byte[] keyNal = null;


    public void setMsgPostOffice(IMsgPostOffice postOffice) {
        this.msgPostOffice = postOffice;
    }


    public void setVideoSize(int width, int height, int pixelFormat) {
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
    }

    public String getBase64SPS() {
//        return "QgAo9AWh6IA=";
        while (sps == null && container.getTaskExecutor().isStartState()) {
            container.getTaskExecutor().waitTask(0);
        }
        return Mp4Analysis.getBase64SPS(sps);
    }

    public String getBase64PPS() {
//        return "zjiA";
        while (pps == null && container.getTaskExecutor().isStartState()) {
            container.getTaskExecutor().waitTask(0);
        }
        return Mp4Analysis.getBase64PPS(pps);
    }

    public String getProfileLevel() {
//        return "0028f4";
        while (sps == null && container.getTaskExecutor().isStartState()) {
            container.getTaskExecutor().waitTask(0);
        }
        return Mp4Analysis.getProfileLevel(sps);
    }

    @Override
    protected boolean onSteamBeginInit() {
        LogDog.w("--> DVR H264Stream start ing ....");
        //初始化摄像头
        ts = 0;
        boolean isOpen = H264Preview.openCamera(width, height, pixelFormat);
        if (isOpen) {
            try {
                //开启异步线程处理发送数据
                IConsumerTaskExecutor executor = container.getTaskExecutor();
                executor.startAsyncProcessData();
            } catch (Exception e) {
                e.printStackTrace();
                stopSteam();
            }
        } else {
            LogDog.e("--> DVR H264Stream open error !");
            stopSteam();
        }
        return false;
    }

    @Override
    protected void onSteamEndInit() {
        if (!steamTrigger) {
            LogDog.d("==>DecodeMP4H264Stream 等待 RTSP播放指令!");
            taskContainer.getTaskExecutor().waitTask(0);
            LogDog.d("==>DecodeMP4H264Stream 收到 RTSP播放指令!");
        }
    }

    @Override
    protected void onSteamCreateNalData() {
        IConsumerAttribute<NalPacket> attribute = container.getAttribute();
        oldTime = System.nanoTime();
        byte[] nal = H264Preview.getH264();
        if (nal == null) {
            stopSteam();
            return;
        }

        int skipLength;
        ts += duration;

        NalAnalysis.NalInfo nalInfo = NalAnalysis.analysis(nal);
        if (nalInfo == null) {
            return;
        }
        if (nalInfo.isHasPPSAndSPS()) {
            sps = nalInfo.getSPS();
            pps = nalInfo.getPPS();
            keyNal = nal;
        }

        System.arraycopy(nal, 0, header, 0, header.length);
        skipLength = 5;


        if (nal.length <= nalMaxLength) {
            ContentPacket contentPacket = contentPacketCache.getRepeatData();
            if (contentPacket == null) {
                contentPacket = new ContentPacket(RtpPacket.MTU);
            }
            contentPacket.setTime(ts);
            byte[] data = contentPacket.getData();
            data[RtpPacket.RTP_HEADER_LENGTH] = header[4];
            skipLength--;
            System.arraycopy(nal, skipLength, data, RtpPacket.RTP_HEADER_LENGTH + 1, nal.length - skipLength);
            LogDog.i("==> single rtp = " + byteToHexStr(data));
            contentPacket.setFullNal(true);
            contentPacket.setLimit(nal.length - skipLength + RtpPacket.RTP_HEADER_LENGTH);
            if (steamTrigger) {
                attribute.pushToCache(contentPacket);
                resumeTask();
            }
            contentPacketCache.setRepeatData(contentPacket);
        } else {
            header[1] = (byte) (header[4] & 0x1F);  // FU header type
            header[1] |= 0x80; // Start bit
            header[0] = (byte) (header[4] & 0x60); // FU indicator NRI
            header[0] |= 28;

            while (skipLength < nal.length) {
                ContentPacket contentPacket = contentPacketCache.getRepeatData();
                if (contentPacket == null) {
                    contentPacket = new ContentPacket(RtpPacket.MTU);
                }
                byte[] data = contentPacket.getData();
                contentPacket.setFullNal(false);

                data[RtpPacket.RTP_HEADER_LENGTH] = header[0];
                data[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];

                int len = nal.length - skipLength > nalMaxLength ? nalMaxLength : nal.length - skipLength;
                System.arraycopy(nal, skipLength, data, RtpPacket.RTP_HEADER_LENGTH + 2, len);
//                    LogDog.i("==> long rtp = " + Utils.bytes2HexString(data));
                skipLength += len;
                // Last parameterSetPacket before next NAL
                if (nal.length == skipLength) {
                    // End bit on
                    contentPacket.setFullNal(true);
                    data[RtpPacket.RTP_HEADER_LENGTH + 1] |= 0x40;
                }
                contentPacket.setLimit(RtpPacket.RTP_HEADER_LENGTH + 2 + len);
                contentPacket.setTime(ts);
                if (steamTrigger) {
                    attribute.pushToCache(contentPacket);
                    resumeTask();
                }
                contentPacketCache.setRepeatData(contentPacket);
                // Switch start bit
                header[1] = (byte) (header[1] & 0x7F);
            }
        }
        duration = System.nanoTime() - oldTime;
    }

    @Override
    protected void onSteamDestroy() {
        LogDog.w("--> DVR H264Stream  stop ing ....");
        H264Preview.stopCamera();
    }

    public byte[] getKeyNal() {
        return keyNal;
    }

//    public List<NalPacket> getKeyNalToNalPacket() {
//        RecorderTask recorderTask = container.getTask();
//        return recorderTask.packetNal(keyNal);
//    }


    //        private List<NalPacket> packetNal(byte[] nal) {
//            if (nal == null) {
//                return null;
//            }
//            List<NalPacket> result = new ArrayList<>();
//            byte[] header = new byte[5];
//            long ts = 0;
//            int skipLength;
//
//            System.arraycopy(nal, 0, header, 0, header.length);
//            skipLength = 5;
//
//
//            if (nal.length <= maxLength) {
//                ContentPacket contentPacket = new ContentPacket(RtpPacket.MTU);
//                contentPacket.setTime(ts);
//                byte[] data = contentPacket.getData();
//                data[RtpPacket.RTP_HEADER_LENGTH] = header[4];
//                skipLength--;
//                System.arraycopy(nal, skipLength, data, RtpPacket.RTP_HEADER_LENGTH + 1, nal.length - skipLength);
//                LogDog.i("==> single rtp = " + byteToHexStr(data));
//                contentPacket.setFullNal(true);
//                contentPacket.setLimit(nal.length - skipLength + RtpPacket.RTP_HEADER_LENGTH);
//                if (trigger) {
//                    IConsumerAttribute<NalPacket> executor = container.getAttribute();
//                    executor.pushToCache(contentPacket);
//                    resumeTask();
//                }
//            } else {
//                header[1] = (byte) (header[4] & 0x1F);  // FU header type
//                header[1] |= 0x80; // Start bit
//                header[0] = (byte) (header[4] & 0x60); // FU indicator NRI
//                header[0] |= 28;
//
//                while (skipLength < nal.length) {
//                    ContentPacket contentPacket = new ContentPacket(RtpPacket.MTU);
//                    byte[] data = contentPacket.getData();
//                    contentPacket.setFullNal(false);
//
//                    data[RtpPacket.RTP_HEADER_LENGTH] = header[0];
//                    data[RtpPacket.RTP_HEADER_LENGTH + 1] = header[1];
//
//                    int len = nal.length - skipLength > maxLength ? maxLength : nal.length - skipLength;
//                    System.arraycopy(nal, skipLength, data, RtpPacket.RTP_HEADER_LENGTH + 2, len);
////                    LogDog.i("==> key rtp = " + Utils.bytes2HexString(data));
//                    skipLength += len;
//                    // Last parameterSetPacket before next NAL
//                    if (nal.length == skipLength) {
//                        // End bit on
//                        contentPacket.setFullNal(true);
//                        data[RtpPacket.RTP_HEADER_LENGTH + 1] |= 0x40;
//                    }
//                    contentPacket.setLimit(RtpPacket.RTP_HEADER_LENGTH + 2 + len);
//                    contentPacket.setTime(ts);
//                    result.add(contentPacket);
//                    // Switch start bit
//                    header[1] = (byte) (header[1] & 0x7F);
//                }
//            }
//            return result;
//        }


    private void resumeTask() {
        IConsumerTaskExecutor executor = container.getTaskExecutor();
        ILoopTaskExecutor asyncExecutor = executor.getAsyncTaskExecutor();
        if (asyncExecutor != null) {
            asyncExecutor.resumeTask();
        } else {
            container.getTaskExecutor().resumeTask();
        }
    }

    private String byteToHexStr(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < b.length; n++) {
            String tmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((tmp.length() == 1) ? "0" + tmp : tmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

}
