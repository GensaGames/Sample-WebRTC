package com.gensagames.samplewebrtc.view.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.controller.BTMonitorController;
import com.gensagames.samplewebrtc.controller.BTRecyclerAdapter;
import com.gensagames.samplewebrtc.controller.SignalingMsgAdapter;
import com.gensagames.samplewebrtc.model.SignalingMessageItem;

public class CallSessionFragment extends Fragment {


    private RecyclerView mRecyclerView;
    private SignalingMsgAdapter mBTRecyclerAdapter;

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_session, null);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.fragmentRecyclerView);
        return view;
    }
}
