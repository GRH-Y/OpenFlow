package flow.core;

import connect.network.nio.NioClientTask;

import java.nio.channels.SocketChannel;

/**
 * FlowClient 客户端
 */
public class FlowClient extends NioClientTask {

    protected OpenFlow openFlow;

    public FlowClient(SocketChannel channel, OpenFlow openFlow) {
        setChannel(channel);
        this.openFlow = openFlow;
    }

}
