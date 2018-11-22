package server.avedcoder.packet;

/**
 * NalPacket
 * Created by dell on 9/12/2017.
 */

public class NalPacket {
    private byte[] data ;//nal数据
    private PacketType packetType = PacketType.VIDEO;
    private int limit = 0;//该nal实际大小
    private boolean isFullNal = false;
    private long time = 0;//该nal产生的时间
//    private byte header = 0;

    public enum PacketType {
        AUDIO, VIDEO
    }

    public NalPacket(int packetSize) {
        data = new byte[packetSize];
        limit = packetSize;
    }

    public NalPacket(byte[] sps, byte[] pps, int headerLength) {
        // STAP-A NAL header + NALU 1 (SPS) size + NALU 2 (PPS) size = 5 bytes
        data = new byte[sps.length + pps.length + 5 + headerLength];
        setStreamParameters(sps, pps);
        limit = data.length;
    }

    public void setFullNal(boolean fullNal) {
        isFullNal = fullNal;
    }

    public boolean isFullNal() {
        return isFullNal;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    //    public void setHeader(byte header)
//    {
//        this.header = header;
//    }
//
//    public byte getHeader()
//    {
//        return header;
//    }

    public void setPacketType(PacketType type) {
        this.packetType = type;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public void init(byte[] sps, byte[] pps) {
        setStreamParameters(sps, pps);
    }

    private void setStreamParameters(byte[] sps, byte[] pps) {
        // A STAP-A NAL (NAL packetType 24) containing the sps and pps of the stream
        if (pps != null && sps != null) {
            // STAP-A NAL header inputStream 24
            data[12] = 24;

            // Write NALU 1 size into the array (NALU 1 inputStream the SPS).
            data[13] = (byte) (sps.length >> 8);
            data[14] = (byte) (sps.length & 0xFF);

            // Write NALU 2 size into the array (NALU 2 inputStream the PPS).
            data[sps.length + 15] = (byte) (pps.length >> 8);
            data[sps.length + 16] = (byte) (pps.length & 0xFF);

            // Write NALU 1 into the array, then write NALU 2 into the array.
            System.arraycopy(sps, 0, data, 15, sps.length);
            System.arraycopy(pps, 0, data, 17 + sps.length, pps.length);
        }
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public byte[] getData() {
        return data;
    }

    public void release() {
        data = null;
        try {
            finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}