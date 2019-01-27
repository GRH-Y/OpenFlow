package server.avdecode.audio;

import server.avdecode.NalSteam;

public class AudioSteam extends NalSteam {

    @Override
    protected boolean onSteamBeginInit() {
        return false;
    }

    @Override
    protected void onSteamEndInit() {

    }

    @Override
    protected void onSteamCreateNalData() {

    }

    @Override
    protected void onSteamDestroy() {

    }
}
