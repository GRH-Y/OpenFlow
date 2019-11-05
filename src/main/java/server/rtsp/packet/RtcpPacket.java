package server.rtsp.packet;

public class RtcpPacket {

    public static final int PACKET_LENGTH = 28;
    private byte[] rtcpData = new byte[PACKET_LENGTH];
    private int mOctetCount = 0, mPacketCount = 0;
    private long interval = 5000, delta, now, oldnow;

    public RtcpPacket(int ssrc) {
        /*							         Version(2)  Padding(0)				*/
        /*									     ^		  ^			PT = 0	    */
        /*									     |		  |				^		*/
        /*									     | --------			 	|		*/
        /*									     | |---------------------		*/
        /*							 		     | ||							*/
        /*								         | ||							*/
//        rtcpData[0] = (byte) Integer.parseInt("10000000", 2);
        rtcpData[0] = (byte) 0x80;

        /* Packet Type PT */
        rtcpData[1] = (byte) 200;

        /* Byte 2,3          ->  Length		                     */
        setLong(PACKET_LENGTH / 4 - 1, 2, 4);

        /* Byte 4,5,6,7      ->  SSRC                            */
        setLong(ssrc, 4, 8);
        /* Byte 8,9,10,11    ->  NTP timestamp hb				 */
        /* Byte 12,13,14,15  ->  NTP timestamp lb				 */
        /* Byte 16,17,18,19  ->  RTP timestamp		             */
        /* Byte 20,21,22,23  ->  packet numCount				 	 */
        /* Byte 24,25,26,27  ->  octet numCount			         */
    }

    /**
     * Sets the temporal interval between two RTCP Sender Reports.
     * Default interval inputStream set to 3 seconds.
     * Set 0 to disable RTCP.
     *
     * @param interval The interval in milliseconds
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }


    /**
     * Updates the number of packets sent, and the total amount of data sent.
     *
     * @param length The length of the packet
     * @param rtpts  The RTP timestamp.
     **/
    public byte[] updateData(int length, long rtpts) {
        mPacketCount += 1;
        mOctetCount += length;
        setLong(mPacketCount, 20, 24);
        setLong(mOctetCount, 24, 28);

//        now = SystemClock.elapsedRealtime();
        now = System.currentTimeMillis();
        delta += oldnow != 0 ? now - oldnow : 0;
        oldnow = now;
        if (interval > 0 && delta >= interval) {
            // We send a Sender Report
            setRtcpData(now, rtpts);
            delta = 0;
            return rtcpData;
        }
        return null;
    }

    /**
     * Resets the reports (total number of bytes sent, number of packets sent, etc.)
     */
    public void reset() {
        mPacketCount = 0;
        mOctetCount = 0;
        setLong(mPacketCount, 20, 24);
        setLong(mOctetCount, 24, 28);
        delta = now = oldnow = 0;
    }

    /**
     * Sends the RTCP packet over the network.
     *
     * @param ntpts the NTP timestamp.
     * @param rtpts the RTP timestamp.
     */
    private void setRtcpData(long ntpts, long rtpts) {
        long hb = ntpts / 1000000000;
//        long lb = ((ntpts - hb * 1000000000) * 4294967296L) / 1000000000;
        long lb = ntpts - hb;
        setLong(hb, 8, 12);
        setLong(lb, 12, 16);
        setLong(rtpts, 16, 20);
    }


    private void setLong(long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            rtcpData[end] = (byte) (n % 256);
            n >>= 8;
        }
    }
}
