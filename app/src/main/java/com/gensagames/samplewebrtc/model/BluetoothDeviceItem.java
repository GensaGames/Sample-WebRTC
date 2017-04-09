package com.gensagames.samplewebrtc.model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.Serializable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BluetoothDeviceItem implements Serializable {
    private String deviceName;
    private String defaultName;
    private String deviceAddress;

    public BluetoothDeviceItem(@NonNull BluetoothDevice device,
                               @Nullable String defaultName) {
        this(device.getName(), device.getAddress(), defaultName);
    }

    public BluetoothDeviceItem(@NonNull String deviceName, @NonNull String deviceAddress,
                               @Nullable String defaultName) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;

        if (TextUtils.isEmpty(deviceName)) {
            this.defaultName = defaultName;
        }
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public static BluetoothDeviceItem createFromBT (BluetoothDevice device, String defaultName) {
        return new BluetoothDeviceItem(device, defaultName);
    }

}
