package com.gensagames.samplewebrtc.signaling;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gensagames.samplewebrtc.signaling.helper.OnBluetoothResponse;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BluetoothMonitorController {

    private static final String TAG = BluetoothMonitorController.class.getSimpleName();

    private static final int REQUEST_BLUETOOTH = 707;
    private static final long KEEP_SEARCH_ALIVE = java.util.concurrent.
            TimeUnit.SECONDS.toMillis(30);

    private Handler mLocalUiHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private OnBluetoothResponse mOnBluetoothResponse;

    public BluetoothMonitorController(OnBluetoothResponse onBluetoothResponse) {
        mOnBluetoothResponse = onBluetoothResponse;
        mLocalUiHandler = new Handler(Looper.getMainLooper());
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startSearch (Activity activity) {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth?");
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        } else {
            mBluetoothAdapter.startDiscovery();
        }
    }

    public void cancelSearch () {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth?");
            return;
        }
        mBluetoothAdapter.cancelDiscovery();
    }

    public void registerMonitor (Activity activity) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mBluetoothReceiver, filter);
    }

    public void unRegisterMonitor (Activity activity) {
        activity.unregisterReceiver(mBluetoothReceiver);
    }


    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "Receiving action: " + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mOnBluetoothResponse.onDiscovery(device);
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mLocalUiHandler.removeCallbacks(mTaskAlive);
                mOnBluetoothResponse.onDiscoveryFinished();
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mLocalUiHandler.postDelayed(mTaskAlive, KEEP_SEARCH_ALIVE);
                mOnBluetoothResponse.onDiscoveryStarted();
            }
        }
    };

    private final Runnable mTaskAlive = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "Action to stop searching!");
            cancelSearch();
        }
    };
}
