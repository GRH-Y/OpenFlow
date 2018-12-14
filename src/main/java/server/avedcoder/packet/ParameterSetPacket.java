package server.avedcoder.packet;

/**
 * Created by dell on 9/12/2017.
 */

public class ParameterSetPacket extends NalPacket {
    public ParameterSetPacket(byte[] sps, byte[] pps, int headerLength) {
        super(sps, pps, headerLength);
    }
}
