package com.gensagames.samplewebrtc.controller;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BTRecyclerAdapter extends RecyclerView.Adapter
        <BTRecyclerAdapter.BluetoothDeviceHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private List<BluetoothDeviceItem> mBtDeviceItemList;
    private OnItemClickListener mOnItemClickListener;

    public BTRecyclerAdapter() {
        mBtDeviceItemList = new ArrayList<>();
    }

    @Override
    public BluetoothDeviceHolder onCreateViewHolder
            (ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_bluetooth_item, parent, false);
        return new BluetoothDeviceHolder(itemView,
                mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(BluetoothDeviceHolder holder, int position) {
        holder.bind(mBtDeviceItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return mBtDeviceItemList.size();
    }

    public List<BluetoothDeviceItem> getWorkingItems () {
        return mBtDeviceItemList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }


    @SuppressWarnings("WeakerAccess")
    public static class BluetoothDeviceHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView deviceName;
        public TextView deviceInfo;
        public BTRecyclerAdapter.OnItemClickListener onItemClickListener;

        public BluetoothDeviceHolder(View itemView, BTRecyclerAdapter.
                OnItemClickListener onItemClickListener) {
            super(itemView);
            itemView.setOnClickListener(this);

            this.onItemClickListener = onItemClickListener;
            deviceName = (TextView) itemView.findViewById(R.id.adapterDeviceName);
            deviceInfo = (TextView) itemView.findViewById(R.id.adapterDeviceInfo);
        }

        public void bind (BluetoothDeviceItem item) {
            String deviceNameString = TextUtils.isEmpty(item.getDeviceName()) ?
                    deviceName.getContext().getString(R.string.name_unknown) : item.getDeviceName();

            deviceName.setText(deviceNameString);
            deviceInfo.setText(item.getDeviceAddress());
        }

        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(getAdapterPosition());
            }
        }
    }
}
