package com.gensagames.samplewebrtc.signaling.helper;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface OnBluetoothResponse {
    void onDiscoveryStarted ();
    void onDiscoveryFinished ();
    void onDiscovery(@NonNull BluetoothDevice device );
}
