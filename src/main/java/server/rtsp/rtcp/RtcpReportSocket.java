/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package server.rtsp.rtcp;


import connect.network.udp.JavUdpConnect;

/**
 * Implementation of Sender Report RTCP packets.
 * （RTCP 提供数据分发质量反馈信息，这是 RTP 作为传输协议的部分功能并且它涉及到了其它传输协议的流控制和拥塞控制。）
 */
public class RtcpReportSocket {

    private static final int PACKET_LENGTH = 28;
    private JavUdpConnect connect = null;
    private byte[] mBuffer = new byte[PACKET_LENGTH];
    private int mOctetCount = 0, mPacketCount = 0;
    private long interval = 5000, delta, now, oldnow;


    public RtcpReportSocket(String ip, int port, int ssrc) {
        /*							    Version(2)  Padding(0)					 					*/
        /*									 ^		  ^			PT = 0	    						*/
        /*									 |		  |				^								*/
        /*									 | --------			 	|								*/
        /*									 | |---------------------								*/
        /*									 | ||													*/
        /*								     | ||													*/
        mBuffer[0] = (byte) Integer.parseInt("10000000", 2);

        /* Packet Type PT */
        mBuffer[1] = (byte) 200;

        /* Byte 2,3          ->  Length		                     */
        setLong(PACKET_LENGTH / 4 - 1, 2, 4);

        /* Byte 4,5,6,7      ->  SSRC                            */
        setLong(ssrc, 4, 8);
        /* Byte 8,9,10,11    ->  NTP timestamp hb				 */
        /* Byte 12,13,14,15  ->  NTP timestamp lb				 */
        /* Byte 16,17,18,19  ->  RTP timestamp		             */
        /* Byte 20,21,22,23  ->  packet count				 	 */
        /* Byte 24,25,26,27  ->  octet count			         */

        connect = new JavUdpConnect(ip, port);
    }

    public void startConnect() {
        connect.startConnect();
    }

    public void stopConnect() {
        connect.stopConnect();
    }

    public void setDestination(String ip, int port) {
        connect.refreshDestAddress(ip, port);
    }

    public void setDestination(int port) {
        connect.refreshDestAddress(port);
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
    public void update(int length, long rtpts) {
        mPacketCount += 1;
        mOctetCount += length;
        setLong(mPacketCount, 20, 24);
        setLong(mOctetCount, 24, 28);

//        now = SystemClock.elapsedRealtime();
        now = System.nanoTime();
        delta += oldnow != 0 ? now - oldnow : 0;
        oldnow = now;
        if (interval > 0 && delta >= interval) {
            // We send a Sender Report
            send(System.nanoTime(), rtpts);
            delta = 0;
        }
    }

    public int getLocalPort() {
        return connect.getLocalPort();
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

    private void setLong(long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            mBuffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }

    /**
     * Sends the RTCP packet over the network.
     *
     * @param ntpts the NTP timestamp.
     * @param rtpts the RTP timestamp.
     */
    private void send(long ntpts, long rtpts) {
        long hb = ntpts / 1000000000;
        long lb = ((ntpts - hb * 1000000000) * 4294967296L) / 1000000000;
        setLong(hb, 8, 12);
        setLong(lb, 12, 16);
        setLong(rtpts, 16, 20);
        connect.putSendData(mBuffer);
    }


}
