package com.gensagames.samplewebrtc.signaling.helper;

import com.gensagames.samplewebrtc.signaling.BluetoothConnectivityService;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface ConnectivityChangeListener {
    void onConnectivityStateChanged (BluetoothConnectivityService.ConnectionState state);
}
