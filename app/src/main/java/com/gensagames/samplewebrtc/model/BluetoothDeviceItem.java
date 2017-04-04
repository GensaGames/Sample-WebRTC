package com.gensagames.samplewebrtc.model;

import android.bluetooth.BluetoothDevice;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.controller.BluetoothRecyclerAdapter;

import java.io.Serializable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BluetoothDeviceItem  implements Serializable {
    private String deviceName;
    private String deviceInfo;
    private BluetoothDevice device;

    public BluetoothDeviceItem(BluetoothDevice device) {
        this.deviceName = device.getName();
        this.deviceInfo = device.getAddress();
        this.device = device;
    }

    public String getDeviceName() {
        return deviceName;
    }


    @SuppressWarnings("WeakerAccess")
    public String getDeviceInfo() {
        return deviceInfo;
    }

    public BluetoothDevice getBluetoothDevice () {
        return device;
    }

    @SuppressWarnings("WeakerAccess")
    public static class BluetoothDeviceHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView deviceName;
        public TextView deviceInfo;
        public BluetoothRecyclerAdapter.OnItemClickListener onItemClickListener;

        public BluetoothDeviceHolder(View itemView, BluetoothRecyclerAdapter.
                OnItemClickListener onItemClickListener) {
            super(itemView);
            this.onItemClickListener = onItemClickListener;
            deviceName = (TextView) itemView.findViewById(R.id.adapterDeviceName);
            deviceInfo = (TextView) itemView.findViewById(R.id.adapterDeviceInfo);
        }

        public void bind (BluetoothDeviceItem item) {
            deviceName.setText(item.getDeviceName());
            deviceInfo.setText(item.getDeviceInfo());
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }
    }
}
