package flow.core;


import com.sun.istack.internal.NotNull;
import connect.network.nio.NioServerTask;

import java.nio.channels.SocketChannel;

/**
 * FlowServer 推流服务端
 * Created by prolog on 2017/8/9.
 */

public abstract class FlowServer extends NioServerTask {

    protected OpenFlow openFlow;

    protected void setOpenFlow(@NotNull OpenFlow openFlow) {
        this.openFlow = openFlow;
    }

    abstract protected FlowClient onAccept(@NotNull SocketChannel channel);

    @Override
    protected void onAcceptServerChannel(SocketChannel channel) {
        FlowClient client = onAccept(channel);
        FlowNetFactory flowNetFactory = openFlow.getFlowNetFactory();
        flowNetFactory.addTcpClient(client);
    }


    @Override
    protected void onCloseServerChannel() {
        openFlow.getFlowDataSrc().release();
        openFlow.getFlowPushStream().release();
    }

}
