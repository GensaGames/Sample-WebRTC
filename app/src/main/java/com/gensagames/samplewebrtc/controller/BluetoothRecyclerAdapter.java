package com.gensagames.samplewebrtc.controller;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BluetoothRecyclerAdapter extends RecyclerView.Adapter
        <BluetoothDeviceItem.BluetoothDeviceHolder> {

    private List<BluetoothDeviceItem> mBluetoothDeviceItemList;

    public BluetoothRecyclerAdapter() {
        mBluetoothDeviceItemList = new ArrayList<>();
    }

    @Override
    public BluetoothDeviceItem.BluetoothDeviceHolder onCreateViewHolder
            (ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_bluetooth_item, parent, false);
        return new BluetoothDeviceItem.BluetoothDeviceHolder(itemView);
    }

    @Override
    public void onBindViewHolder(BluetoothDeviceItem.BluetoothDeviceHolder holder, int position) {
        holder.bind(mBluetoothDeviceItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return mBluetoothDeviceItemList.size();
    }

    public List<BluetoothDeviceItem> getWorkingItems () {
        return mBluetoothDeviceItemList;
    }
}
