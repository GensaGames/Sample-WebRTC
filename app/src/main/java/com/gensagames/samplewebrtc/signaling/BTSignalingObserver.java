package com.gensagames.samplewebrtc.signaling;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.model.BTMessageItem;
import com.gensagames.samplewebrtc.signaling.helper.ConnectivityChangeListener;
import com.gensagames.samplewebrtc.signaling.helper.MessageObservable;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BTSignalingObserver implements MessageObservable, ConnectivityChangeListener {

    private static final String TAG = BTSignalingObserver.class.getSimpleName();
    private static final int BASE_QUEUE_CAP = 10;

    private Context mLocalContext;
    private final BlockingQueue<Runnable> listActionToDone;
    private final BTConnectivityService mBluetoothService;
    private final BluetoothAdapter mAdapter;
    private String mConnectAddress;

    public BTSignalingObserver(Context context) {
        mLocalContext = context;
        listActionToDone = new ArrayBlockingQueue<>(BASE_QUEUE_CAP);
        mBluetoothService = new BTConnectivityService(this);
        mBluetoothService.setConnectivityChangeListener(this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothService.getState()
                == BTConnectivityService.ConnectionState.IDLE) {
            mBluetoothService.start();
        }
    }

    @Override
    public void onReceiveMsg(byte[] msgBytes, int length) {
        Log.d(TAG, "onReceiveMsg: " + new String(msgBytes));
        handleIncomingMsg(new String(msgBytes, 0, length));

    }

    @Override
    public void onSentMsg(byte[] msgBytes) {
        Log.d(TAG, "onSentMsg: " + new String(msgBytes));
    }


    public void setWorkingAddress(String address) {
        mConnectAddress = address;
    }

    public boolean isConnected () {
        return mBluetoothService.getState() == BTConnectivityService.
                ConnectionState.STATE_CONNECTED;
    }
    public BluetoothDevice getWorkingDevice() {
        return mBluetoothService.getWorkingDevice();
    }

    public boolean sendWhenReady(@NonNull final Object object) {
        final String msg;
        if (object instanceof String) {
            msg = (String) object;
        } else {
            msg = new Gson().toJson(object);
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
    public void onConnectivityStateChanged(BTConnectivityService.ConnectionState state) {
        switch (state) {
            case STATE_CONNECTED:
            case STATE_LISTEN:
            case IDLE:
                continueActions();
        }
    }

    private void handleIncomingMsg (String msg) {
        try {
            Gson gson = new Gson();
            BTMessageItem btMsg =
                    gson.fromJson(msg, BTMessageItem.class);
            Intent intent = null;
            if (btMsg.getMessageType() ==
                    BTMessageItem.MessageType.SDP_EXCHANGE) {
                switch (btMsg.getWorkingSdp().type) {
                    case OFFER:
                        intent = new Intent(VoIPEngineService.ACTION_OFFER_SDP, Uri.EMPTY,
                                mLocalContext, VoIPEngineService.class);
                        break;
                    case ANSWER:
                        intent = new Intent(VoIPEngineService.ACTION_ANSWER_SDP, Uri.EMPTY,
                                mLocalContext, VoIPEngineService.class);
                        break;
                }
            }
            if (intent != null) {
                intent.putExtra(VoIPEngineService.EXTRA_BT_MSG, btMsg);
                mLocalContext.startService(intent);
            }
        }
        catch (JsonSyntaxException ex) {
            Log.e(TAG, "Wrong representation of Call Msg!", ex);
        }
    }


    private boolean continueActions () {
        if (mBluetoothService.getState() ==
                BTConnectivityService.ConnectionState.IDLE) {
            mBluetoothService.start();
            return false;
        }
        Runnable nextAction = listActionToDone.poll();
        if (nextAction == null) {
            return false;
        }
        if (mBluetoothService.getState() !=
                BTConnectivityService.ConnectionState.STATE_CONNECTED) {
            mBluetoothService.connect(mAdapter.getRemoteDevice(mConnectAddress));
            return false;
        }

        try {
            nextAction.run();
        } catch (Exception ex) {
            Log.e(TAG, "Error on continueActions!", ex);
        }
        return true;
    }
}
