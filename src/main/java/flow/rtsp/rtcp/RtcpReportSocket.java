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

package flow.rtsp.rtcp;


import connect.network.base.NetTaskStatus;
import connect.network.udp.UdpServerTask;
import flow.rtsp.packet.RtcpPacket;
import flow.rtsp.rtp.RtpSender;

/**
 * Implementation of Sender Report RTCP packets.
 * （RTCP 提供数据分发质量反馈信息，这是 RTP 作为传输协议的部分功能并且它涉及到了其它传输协议的流控制和拥塞控制。）
 */
public class RtcpReportSocket extends UdpServerTask {


    public RtcpReportSocket(String bindAddress, int bindPort) {
        setAddress(bindAddress, bindPort);
        setSender(new RtpSender());
    }


    public void sendRtcpData(RtcpPacket packet, int length, long rtpts) {
        byte[] data = packet.updateData(length, rtpts);
        getSender().sendData(data);
    }

    public int getLocalPort() {
        if (getTaskStatus() == NetTaskStatus.NONE) {
            return 0;
        }
        while (getSocket() == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return getSocket().getLocalPort();
    }

}
