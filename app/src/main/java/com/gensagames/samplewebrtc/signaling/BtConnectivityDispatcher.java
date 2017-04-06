package com.gensagames.samplewebrtc.signaling;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.util.Log;

import com.gensagames.samplewebrtc.signaling.helper.ConnectivityChangeListener;
import com.gensagames.samplewebrtc.signaling.helper.OnMessageObservable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BtConnectivityDispatcher implements OnMessageObservable, ConnectivityChangeListener {

    private static final String TAG = BtConnectivityDispatcher.class.getSimpleName();
    private static final int BASE_QUEUE_CAP = 10;

    private final BlockingQueue<Runnable> listActionToDone;
    private final BtConnectivityService mBluetoothService;
    private final BluetoothAdapter mAdapter;
    private BluetoothDevice mBluetoothDevice;

    public BtConnectivityDispatcher() {
        listActionToDone = new ArrayBlockingQueue<>(BASE_QUEUE_CAP);
        mBluetoothService = new BtConnectivityService(this);
        mBluetoothService.setConnectivityChangeListener(this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothService.getState()
                == BtConnectivityService.ConnectionState.IDLE) {
            mBluetoothService.start();
        }
    }

    public void setWorkingAddress (String address) {
        if (mAdapter != null) {
            mBluetoothDevice = mAdapter.getRemoteDevice(address);
        } else {
            Log.e(TAG, "Device doesn't support Bluetooth?");
        }
    }

    public boolean sendWhenReady(@NonNull final String msg) {
        if (mBluetoothDevice == null) {
            Log.e(TAG, "Wrong BluetoothDevice! Return!");
            return false;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mBluetoothService.write(msg.getBytes());
            }
        };
        return listActionToDone.offer(runnable) && continueActions();
    }

    @Override
    public void onReceiveMsg(byte[] msgBytes, int length) {
        Log.d(TAG, "onReceiveMsg: " + new String(msgBytes));

    }

    @Override
    public void onSentMsg(byte[] msgBytes) {
        Log.d(TAG, "onSentMsg: " + new String(msgBytes));

    }

    @Override
    public void onConnectivityStateChanged(BtConnectivityService.ConnectionState state) {
        switch (state) {
            case IDLE:
                break;
            case STATE_LISTEN:
                break;
            case STATE_CONNECTED:
                continueActions();
        }
    }


    private boolean continueActions () {
        if (mBluetoothService.getState() !=
                BtConnectivityService.ConnectionState.STATE_CONNECTED) {
            mBluetoothService.connect(mBluetoothDevice);
            return false;
        }

        try {
            Runnable nextAction = listActionToDone.poll();
            if (nextAction != null) {
                nextAction.run();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error on continueActions!", ex);
        }
        return true;
    }
}
