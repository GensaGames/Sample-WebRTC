package com.gensagames.samplewebrtc.controller;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.model.SignalingMessageItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GensaGames
 * GensaGames
 */

public class SignalingMsgAdapter extends RecyclerView.Adapter<SignalingMsgAdapter.SignalingMsgHolder>  {

    private List<SignalingMessageItem> mSignalingMsgList;

    public SignalingMsgAdapter() {
        mSignalingMsgList = new ArrayList<>();
    }

    public List<SignalingMessageItem> getWorkingItems() {
        return mSignalingMsgList;
    }

    @Override
    public SignalingMsgHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_signaling_item, parent, false);
        return new SignalingMsgHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SignalingMsgHolder holder, int position) {
        holder.bind(mSignalingMsgList.get(position));
    }

    @Override
    public int getItemCount() {
        return mSignalingMsgList.size();
    }

    @SuppressWarnings("WeakerAccess")
    public static class SignalingMsgHolder extends RecyclerView.ViewHolder {
        public TextView msgView;
        public BTRecyclerAdapter.OnItemClickListener onItemClickListener;

        public SignalingMsgHolder(View itemView) {
            super(itemView);
            msgView = (TextView) itemView.findViewById(R.id.adapterSignalingMsg);
        }
        public void bind (SignalingMessageItem item) {
            String msg = new Gson().toJson(item);
            msgView.setText(SignalingMessageItem.formatString(msg));
        }
    }
}
