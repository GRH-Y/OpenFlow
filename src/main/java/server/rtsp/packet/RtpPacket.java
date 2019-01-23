package server.rtsp.packet;

import java.util.Random;

/**
 * RtpPacket (nal封装成rtp协议包)
 * Created by dell on 9/8/2017.
 */

public class RtpPacket {
    public static final int MTU = 1400;
    public static final int RTP_HEADER_LENGTH = 12;

    private byte[] data = null;
    private long clock = 90000;
    private int limit = 0;
    private int seq = 0;
    private int ssrc = new Random().nextInt();
    private boolean isFullNal = false;

    private PacketType packetType = PacketType.VIDEO;


    public void setPacketType(PacketType type) {
        this.packetType = type;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    /**
     * Sets the clock frequency of the stream in Hz.
     */
    public void setClockFrequency(long clock) {
        this.clock = clock;
    }

    public long getClockFrequency() {
        return clock;
    }


    public void setNalPacket(NalPacket packet) {
        data = packet.getData();
        limit = packet.getLimit();
        //1-2
        data[0] = (byte) 0x80;
        data[1] = (byte) 96 & 0x7F;
        if (isFullNal) {
            //设置最后的包标记
            data[1] |= 0x80;
        }
        //2-4
        setLong(data, ++seq, 2, 4);
        isFullNal = packet.isFullNal();
        //4-8
        setLong(data, packet.getTime() / 10000L, 4, 8);
//        setLong(data, (timestamp / 100L) * (clock / 1000L) / 10000L, 4, 8);
//        setLong(data, (timestamp + clock / 30) / 10000L, 4, 8);
        //8-12
        setLong(data, ssrc, 8, 12);
    }


    public int getSSRC() {
        return ssrc;
    }

    public byte[] getData() {
        return data;
    }

    public int getLimit() {
        return limit;
    }

    private void setLong(byte[] buffer, long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }
}
