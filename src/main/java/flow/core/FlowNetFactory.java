package flow.core;

import connect.network.base.joggle.INetFactory;
import connect.network.nio.NioClientFactory;
import connect.network.nio.NioClientTask;
import connect.network.nio.NioServerFactory;
import connect.network.nio.NioServerTask;
import connect.network.udp.UdpFactory;
import connect.network.udp.UdpTask;

public class FlowNetFactory {

    private INetFactory<UdpTask> udpFactory;
    private INetFactory<NioServerTask> serverFactory;
    private INetFactory<NioClientTask> clientFactory;

    private FlowServer flowServer;

    public FlowNetFactory() {
        udpFactory = new UdpFactory();
        clientFactory = new NioClientFactory();
        serverFactory = new NioServerFactory();
    }

    public <T extends FlowServer> T getFlowServer() {
        return (T) flowServer;
    }

    /**
     * 设置rtsp服务地址和端口
     *
     * @param server rtsp服务
     */
    public void setFlowServer(FlowServer server) {
        this.flowServer = server;
    }

    protected void open() {
        udpFactory.open();
        clientFactory.open();
        serverFactory.open();
        serverFactory.addTask(flowServer);
    }

    protected void close() {
        serverFactory.removeTask(flowServer);
        serverFactory.close();
        clientFactory.close();
        udpFactory.close();
    }

    protected boolean isOpen() {
        return udpFactory.isOpen() || clientFactory.isOpen() || serverFactory.isOpen();
    }

    public void addUdpClient(UdpTask task) {
        udpFactory.addTask(task);
    }

    public void removeUdpClient(UdpTask task) {
        udpFactory.removeTask(task);
    }

    public void addTcpClient(NioClientTask task) {
        clientFactory.addTask(task);
    }

    public void removeTcpClient(NioClientTask task) {
        clientFactory.removeTask(task);
    }

}
