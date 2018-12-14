package server.p2p;


import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import json.JsonUtils;
import server.p2p.bean.AddressBean;
import server.p2p.bean.KeyBean;
import util.LogDog;

/**
 * P2P客户端
 * Created by prolog on 11/23/2016.
 */

public class P2PSClient extends NioClientTask {


    public P2PSClient(String ip, int port) {
        setAddress(ip, port);
        NioSender sender = new NioSender();
        setSender(sender);
        NioReceive receive = new NioReceive(this, "onReceiveData");
        setReceive(receive);
    }

    @Override
    public void onConnectSocketChannel(boolean isConnect) {
        LogDog.i("==> P2PSClient onConnect = " + isConnect);
        KeyBean keyBean = new KeyBean();
        keyBean.setKey("dhu23dh8f3c834fhn24fh919xmb3");
        String json = JsonUtils.toJson(keyBean);
        if (isConnect) {
            getSender().sendData(json.getBytes());
        }
    }

    private void onReceiveData(byte[] data) {
        String json = new String(data);
        LogDog.d("==> NioReceive data = " + json);
        if (json.contains("ip")) {
            AddressBean addressBean = JsonUtils.toEntity(AddressBean.class, json);
            ConnectTask task = new ConnectTask(addressBean);
            NioClientFactory clientFactory = NioClientFactory.getFactory();
            clientFactory.addTask(task);

        }
    }

}
