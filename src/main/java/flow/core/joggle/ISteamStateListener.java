package flow.core.joggle;

public interface ISteamStateListener {

    void onStart(SteamType type);

//    void onPause(SteamType type);
//
//    void onResume(SteamType type);

    void onStop(SteamType type);
}
