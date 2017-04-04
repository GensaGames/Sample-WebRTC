package com.gensagames.samplewebrtc.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
import com.gensagames.samplewebrtc.signaling.BluetoothMonitorController;
import com.gensagames.samplewebrtc.controller.BluetoothRecyclerAdapter;
import com.gensagames.samplewebrtc.signaling.helper.OnBluetoothResponse;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.view.helper.OnSliderPageSelected;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CallListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        OnBluetoothResponse, OnSliderPageSelected, BluetoothRecyclerAdapter.OnItemClickListener {

    private static final String TAG = CallListFragment.class.getSimpleName();

    private View mTextRefresh;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private BluetoothRecyclerAdapter mBluetoothRecyclerAdapter;
    private BluetoothMonitorController mBluetoothMonitorController;

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

        mBluetoothMonitorController = new BluetoothMonitorController(this);
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
        BluetoothDeviceItem item = mBluetoothRecyclerAdapter
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
    }

    @Override
    public void onDiscoveryFinished() {
        mTextRefresh.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public void onDiscovery(@NonNull BluetoothDevice device) {
        mBluetoothRecyclerAdapter.getWorkingItems().add(new
                BluetoothDeviceItem(device));
        mBluetoothRecyclerAdapter.notifyDataSetChanged();

    }

    private void setupAdapter () {
        mSwipeRefreshLayout.setOnRefreshListener(this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mBluetoothRecyclerAdapter = new BluetoothRecyclerAdapter();
        mBluetoothRecyclerAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mBluetoothRecyclerAdapter);
        mBluetoothRecyclerAdapter.notifyDataSetChanged();
    }

    private void startSignalingCall (@NonNull BluetoothDeviceItem device) {
        Activity activityContext = getActivity();
        Intent intent = new Intent(VoIPEngineService.ACTION_START_CALL, Uri.EMPTY,
                activityContext, VoIPEngineService.class);
        intent.putExtra(VoIPEngineService.EXTRA_START_CALL, device);
        activityContext.startService(intent);
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
