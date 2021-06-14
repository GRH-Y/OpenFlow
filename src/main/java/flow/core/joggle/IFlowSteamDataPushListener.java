package flow.core.joggle;

/**
 * NalSteam 监听数据输出
 */
public interface IFlowSteamDataPushListener<T> {

    void onStreamDataPush(T data);
}
