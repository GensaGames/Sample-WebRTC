package com.gensagames.samplewebrtc.model;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.gensagames.samplewebrtc.R;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BluetoothDeviceItem  {
    private String deviceName;
    private String deviceInfo;

    public BluetoothDeviceItem(String deviceName, String deviceInfo) {
        this.deviceName = deviceName;
        this.deviceInfo = deviceInfo;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public static class BluetoothDeviceHolder extends RecyclerView.ViewHolder {
        public TextView deviceName;
        public TextView deviceInfo;

        public BluetoothDeviceHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.adapterDeviceName);
            deviceInfo = (TextView) itemView.findViewById(R.id.adapterDeviceInfo);
        }

        public void bind (BluetoothDeviceItem item) {
            deviceName.setText(item.getDeviceName());
            deviceInfo.setText(item.getDeviceInfo());
        }
    }
}
