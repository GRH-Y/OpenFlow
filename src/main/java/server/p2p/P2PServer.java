package server.p2p;


import connect.json.JsonUtils;
import connect.network.nio.*;
import server.p2p.bean.AddressBean;
import server.p2p.bean.KeyBean;
import util.IoUtils;
import util.Logcat;
import util.NetUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P2PServer p2p服务端
 * Created by prolog on 10/25/2016.
 */

public class P2PServer extends NioServerTask {

    private Map<String, NioClientTask> nioClientTaskMap;

    public P2PServer(int port) throws SocketException {
        this("wlan", port);
    }

    public P2PServer(String netInterface, int port) throws SocketException {
        setAddress(NetUtils.getLocalIp(netInterface), port);
        nioClientTaskMap = new ConcurrentHashMap<>();
    }


    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        Logcat.i("has client connect P2PServer ======>");
        Client client = new Client(channel);
        NioClientFactory serverFactory = NioClientFactory.getFactory();
        serverFactory.open();
        serverFactory.addTask(client);
    }

    @Override
    protected void onOpenServerChannel(boolean isSuccess) {
        Logcat.i("==> P2PServer onConnect = " + isSuccess);
        Logcat.d("==> P2PServer address = " + getHost() + ":" + getPort());
    }


    private class Client extends NioClientTask {


        public Client(SocketChannel channel) {
            super(channel);
            NioSender sender = new NioSender();
            setSender(sender);
            ClientReceive receive = new ClientReceive();
            setReceive(receive);
        }

        @Override
        protected void onConnectSocketChannel(boolean isConnect) {
            Logcat.d("==> NioServer Client onConnect = " + isConnect);
        }

        private class ClientReceive extends NioReceive {

            private void sendAddressInfo(SocketChannel channel, NioSender sender) {
                try {
                    AddressBean addressBean = new AddressBean();
                    InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                    addressBean.setIp(address.getHostName());
                    addressBean.setPort(address.getPort());
                    sender.sendData(JsonUtils.toNewJson(addressBean).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            @Override
            protected boolean onRead(SocketChannel channel) throws IOException {
                byte[] data = IoUtils.tryRead(channel);
                if (data != null) {
                    Logcat.d("==> ClientReceive data = " + new String(data));
                    KeyBean keyBean = JsonUtils.toEntity(KeyBean.class, data);
                    if (nioClientTaskMap.containsKey(keyBean.getKey())) {
                        NioClientTask task = nioClientTaskMap.get(keyBean.getKey());
                        NioSender otherSender = task.getSender();
                        sendAddressInfo(channel, otherSender);
                        SocketChannel otherChannel = task.getSocketChannel();
                        sendAddressInfo(otherChannel, getSender());
                    } else {
                        nioClientTaskMap.put(keyBean.getKey(), Client.this);
                    }
                }
                return true;
            }

        }

    }
}
