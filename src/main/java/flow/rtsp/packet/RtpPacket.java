package flow.rtsp.packet;

/**
 * RtpPacket (nal封装成rtp协议包)
 * Created by dell on 9/8/2017.
 */

public class RtpPacket {
    public static final int MTU = 1400;
    public static final int RTP_HEADER_LENGTH = 12;

    private NalPacket nalPacket;

    public RtpPacket(NalPacket packet) {
        this.nalPacket = packet;
        byte[] data = nalPacket.getData();
        //1-2
        data[0] = (byte) 0x80;
        data[1] = (byte) 96 & 0x7F;
    }

    public void update(int seq) {
        byte[] data = nalPacket.getData();

        if (nalPacket.isFullNal()) {
            //设置最后的包标记
            data[1] |= 0x80;
        }
        //2-4
        setLong(data, seq, 2, 4);

        //4-8
        long timeStamp = nalPacket.getTime();
        //timeStamp / 10000L
        setLong(data, timeStamp, 4, 8);
//        setLong(data, (timestamp / 100L) * (clock / 1000L) / 10000L, 4, 8);
//        setLong(data, (timestamp + clock / 30) / 10000L, 4, 8);
    }

    public void setSSRC(int ssrc) {
        byte[] data = nalPacket.getData();
        //8-12
        setLong(data, ssrc, 8, 12);
    }

    public NalPacket getNalPacket() {
        return nalPacket;
    }

    private void setLong(byte[] buffer, long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }
}
