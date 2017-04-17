package com.gensagames.samplewebrtc.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.controller.BTMonitorController;
import com.gensagames.samplewebrtc.controller.BTRecyclerAdapter;
import com.gensagames.samplewebrtc.model.CallSessionItem;
import com.gensagames.samplewebrtc.signaling.helper.OnBluetoothResponse;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.view.helper.OnSliderPageSelected;

public class CallListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        OnBluetoothResponse, OnSliderPageSelected, BTRecyclerAdapter.OnItemClickListener {

    private static final String TAG = CallListFragment.class.getSimpleName();

    private View mTextRefresh;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private BTRecyclerAdapter mBTRecyclerAdapter;
    private BTMonitorController mBluetoothMonitorController;

    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_list, null);
        mTextRefresh = view.findViewById(R.id.fragmentTextRefresh);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.fragmentRecyclerView);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragmentRefreshLayout);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupAdapter();

        mBluetoothMonitorController = new BTMonitorController(this);
        mBluetoothMonitorController.registerMonitor(getActivity());
    }

    @Override
    public void onDestroyView() {
        mBluetoothMonitorController.unRegisterMonitor(getActivity());
        super.onDestroyView();
    }

    @Override
    public void onRefresh() {
        mBluetoothMonitorController.startSearch(getActivity());
    }

    /**
     * Listening changes, when this page
     * already scrolled and selected
     */
    @Override
    public void onThisPageSelected() {
        mBluetoothMonitorController.startSearch(getActivity());
    }

    /**
     * Recycler Item Clicked
     */
    @Override
    public void onItemClick(int position) {
        Log.d(TAG, "OnItemClick..");
        BluetoothDeviceItem item = mBTRecyclerAdapter
                .getWorkingItems().get(position);
        getDialogForSignalingCall(getString(R.string.dialog_tittle_make_call), getString(R
                .string.dialog_msg_make_call, item.getDeviceName()), item)
                .show();
    }

    /**
     * Bluetooth events.
     * For starting and listening new devices
     */

    @Override
    public void onDiscoveryStarted() {
        mTextRefresh.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(true);
        mBTRecyclerAdapter.getWorkingItems().clear();
        mBTRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDiscoveryFinished() {
        mTextRefresh.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public void onDiscovery(@NonNull BluetoothDevice device) {
        mBTRecyclerAdapter.getWorkingItems().add(BluetoothDeviceItem.
                createFromBT(device, getString(R.string.name_unknown)));
        mBTRecyclerAdapter.notifyDataSetChanged();

    }

    private void setupAdapter () {
        mSwipeRefreshLayout.setOnRefreshListener(this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mBTRecyclerAdapter = new BTRecyclerAdapter();
        mBTRecyclerAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mBTRecyclerAdapter);
        mBTRecyclerAdapter.notifyDataSetChanged();
    }

    private void startSignalingCall (@NonNull BluetoothDeviceItem device) {
        Log.d(TAG, "startSignalingCall to Device!");
        mBluetoothMonitorController.cancelSearch();

        MainSliderFragment fragment = (MainSliderFragment) this.getParentFragment();
        if (fragment != null) {
            fragment.notifyStartCall(device);
        }
    }

    private AlertDialog getDialogForSignalingCall (String tittle, String msg,
                                                   final BluetoothDeviceItem item) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(tittle)
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startSignalingCall(item);
                        dialog.cancel();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
    }


}
