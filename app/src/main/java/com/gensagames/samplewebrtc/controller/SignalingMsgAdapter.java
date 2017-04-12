package com.gensagames.samplewebrtc.controller;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.model.SignalingMessageItem;

/**
 * Created by GensaGames
 * GensaGames
 */

public class SignalingMsgAdapter {

    @SuppressWarnings("WeakerAccess")
    public static class SignalingMsgHolder extends RecyclerView.ViewHolder {
        public TextView msgView;
        public BTRecyclerAdapter.OnItemClickListener onItemClickListener;

        public SignalingMsgHolder(View itemView) {
            super(itemView);
            msgView = (TextView) itemView.findViewById(R.id.adapterDeviceName);
        }

        public void bind (SignalingMessageItem item) {
            msgView.setText(item.toString());
        }
    }
}
