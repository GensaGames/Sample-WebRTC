package com.gensagames.samplewebrtc.controller.helper;

import android.bluetooth.BluetoothDevice;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface OnBluetoothResponse {
    void onDiscoveryStarted ();
    void onDiscoveryFinished ();
    void onDiscovery(BluetoothDevice device );
}
