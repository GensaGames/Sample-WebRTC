package com.gensagames.samplewebrtc.signaling.helper;

import com.gensagames.samplewebrtc.signaling.BTConnectivityService;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface ConnectivityChangeListener {
    void onConnectivityStateChanged (BTConnectivityService.ConnectionState state);
}
