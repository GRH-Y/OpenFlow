package flow.core;

public abstract class OpenFlow {

    private FlowDataSrc flowDataSrc;
    private FlowPushStream flowPushSrc;
    private FlowNetFactory flowNetFactory;

    protected OpenFlow() {
        flowNetFactory = new FlowNetFactory();
        FlowServer flowServer = initFlowServer();
        flowServer.setOpenFlow(this);
        flowNetFactory.setFlowServer(flowServer);
        flowDataSrc = initFlowDataSrc();
        flowPushSrc = initFlowPushStream();
    }

    //-------------------------- init ---------------------------


    protected abstract FlowServer initFlowServer();

    protected abstract FlowDataSrc initFlowDataSrc();

    protected abstract FlowPushStream initFlowPushStream();


    //-------------------------- get ---------------------------

    public <T extends FlowDataSrc> T getFlowDataSrc() {
        return (T) flowDataSrc;
    }

    public <T extends FlowPushStream> T getFlowPushStream() {
        return (T) flowPushSrc;
    }

    public FlowNetFactory getFlowNetFactory() {
        return flowNetFactory;
    }


    //--------------------------startFlow  stopFlow --------------------------

    public void startFlow() {
        if (flowDataSrc == null) {
            throw new RuntimeException(" flowDataSrc is null , must be set flowDataSrc !!! ");
        }
        if (flowDataSrc != null) {
            flowDataSrc.startStream();
        }
        if (!flowNetFactory.isOpen()) {
            flowNetFactory.open();
        }
    }

    public void stopFlow() {
        if (flowNetFactory.isOpen()) {
            flowNetFactory.close();
        }
        if (flowDataSrc != null) {
            flowDataSrc.stopStream();
        }
        clear();
    }


    private void clear() {
        if (flowDataSrc != null) {
            flowDataSrc.release();
        }
        if (flowPushSrc != null) {
            flowPushSrc.release();
        }
    }

}
