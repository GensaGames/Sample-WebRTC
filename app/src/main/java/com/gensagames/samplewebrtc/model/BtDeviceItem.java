package com.gensagames.samplewebrtc.model;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gensagames.samplewebrtc.R;

import java.io.Serializable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BTDeviceItem implements Serializable {
    private String deviceName;
    private String defaultName;
    private String deviceAddress;

    public BTDeviceItem(@NonNull BluetoothDevice device,
                        @Nullable String defaultName) {
        this.deviceName = device.getName();
        this.deviceAddress = device.getAddress();

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

    public static BTDeviceItem createFromBT (BluetoothDevice device, String defaultName) {
        return new BTDeviceItem(device, defaultName);
    }

}
