package server.rtsp.packet;

/**
 * RtpPacket
 * Created by dell on 9/8/2017.
 */

public class RtpPacket {
    public static final int MTU = 1400;
    public static final int RTP_HEADER_LENGTH = 12;

    private byte[] data = null;
    private long clock = 90000;
    private int limit = 0;
    private long time = 0;
    private boolean isFullNal = false;

    private NalPacket.PacketType packetType = NalPacket.PacketType.VIDEO;

//    public RtpPacket()
//    {
//    }

//    public RtpPacket(NalPacket packet, int ssrc)
//    {
//        setNalPacket(packet);
//        setSSRC(ssrc);
//        limit = packet.getLimit();
//    }

    public void setPacketType(NalPacket.PacketType type) {
        this.packetType = type;
    }

    public NalPacket.PacketType getPacketType() {
        return packetType;
    }


    public void setNalPacket(NalPacket packet, int ssrc) {
        setNalPacket(packet);
        setSSRC(ssrc);
//        data[RTP_HEADER_LENGTH] = packet.getHeader();
    }

    public void setNalPacket(NalPacket packet) {
        data = packet.getData();
        data[0] = (byte) 0x80;
        data[1] = (byte) 96 & 0x7F;
        if (isFullNal) {
            data[1] |= 0x80;
        }
        limit = packet.getLimit();
        isFullNal = packet.isFullNal();
        time = packet.getTime();
        updateTimestamp(time);
    }

    public void setSSRC(int ssrc) {
        setLong(data, ssrc, 8, 12);
    }

    public void updateTimestamp(long timestamp) {
        setLong(data, timestamp / 10000L, 4, 8);
//        setLong(data, (timestamp / 100L) * (clock / 1000L) / 10000L, 4, 8);
//        setLong(data, (timestamp + clock / 30) / 10000L, 4, 8);
    }

    /**
     * Sets the marker in the RTP packet.
     */
    public void markNextPacket() {
        if (isFullNal) {
            data[1] |= 0x80;
        }
    }

    public long getTime() {
        return time;
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

    public byte[] getData(long timestamp) {
        updateTimestamp(timestamp);
        return data;
    }

    public byte[] getData() {
        return data;
    }

    public void setSeq(int seq) {
        setLong(data, seq, 2, 4);
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
