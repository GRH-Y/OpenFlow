package server.p2p;


import connect.network.nio.NioClientTask;
import connect.network.nio.NioReceive;
import connect.network.nio.NioSender;
import server.p2p.bean.AddressBean;
import util.Logcat;

public class ConnectTask extends NioClientTask {


    public ConnectTask(AddressBean addressBean) {
        setAddress(addressBean.getIp(), addressBean.getPort());
        NioSender sender = new NioSender();
        setSender(sender);
        NioReceive receive = new NioReceive(this, "onReceiveData");
        setReceive(receive);
    }

    private void onReceiveData(byte[] data) {

    }

    @Override
    public void onConnectSocketChannel(boolean isConnect) {
        Logcat.i("==> InterFlowTask onConnect = " + isConnect);
    }

}
