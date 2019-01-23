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


import connect.network.udp.UdpClientTask;
import connect.network.udp.UdpFactory;
import connect.network.udp.UdpSender;
import server.rtsp.packet.RtcpPacket;

/**
 * Implementation of Sender Report RTCP packets.
 * （RTCP 提供数据分发质量反馈信息，这是 RTP 作为传输协议的部分功能并且它涉及到了其它传输协议的流控制和拥塞控制。）
 */
public class RtcpReportSocket extends UdpClientTask {


    public RtcpReportSocket(String host, int port) {
        setAddress(host, port);
        setSender(new UdpSender());
    }

    public void startConnect() {
        UdpFactory.getFactory().open();
        UdpFactory.getFactory().addTask(this);
    }

    public void stopConnect() {
        UdpFactory.getFactory().removeTask(this);
    }

    public void sendRtcpData(RtcpPacket packet, int length, long rtpts) {
        getSender().sendData(packet.updateData(length, rtpts));
    }

    public int getLocalPort() {
        if (getSocket() == null) {
            return 0;
        }
        return getSocket().getLocalPort();
    }


}
