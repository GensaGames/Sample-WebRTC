package com.gensagames.samplewebrtc.model;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.Serializable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BtDeviceItem implements Serializable {
    private String deviceName;
    private String deviceAddress;

    public BtDeviceItem(@NonNull BluetoothDevice device) {
        this.deviceName = device.getName();
        this.deviceAddress = device.getAddress();
    }
    public BtDeviceItem(@NonNull BluetoothDevice device,
                        @NonNull String defaultName) {
        this(device);
        if (TextUtils.isEmpty(deviceName)) {
            deviceName = defaultName;
        }
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

}
