package com.gensagames.samplewebrtc.signaling.helper;

import com.gensagames.samplewebrtc.signaling.BtConnectivityService;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface ConnectivityChangeListener {
    void onConnectivityStateChanged (BtConnectivityService.ConnectionState state);
}
