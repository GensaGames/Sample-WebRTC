package com.gensagames.samplewebrtc.signaling;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.model.SignalingMessageItem;
import com.gensagames.samplewebrtc.signaling.helper.ConnectivityChangeListener;
import com.gensagames.samplewebrtc.signaling.helper.MessageObservable;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import org.webrtc.SessionDescription;

import java.io.StringReader;
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
    public void onReceiveMsg(String msg) {
        Log.d(TAG, "onReceiveMsg: " + msg);
        handleIncomingMsg(msg);

    }

    @Override
    public void onSentMsg(byte[] msgBytes) {
        Log.d(TAG, "onSentMsg: " + new String(msgBytes));
        continueActions();
    }


    public void connectAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            Log.e(TAG, "Empty address to connect");
            return;
        }
        if (!address.equals(mConnectAddress)
                || !mBluetoothService.getState().isWorking()) {
            mConnectAddress = address;
            mBluetoothService.connect(mAdapter.getRemoteDevice(address));

        }
    }

    public boolean isConnected () {
        return mBluetoothService.getState() == BTConnectivityService.
                ConnectionState.STATE_CONNECTED;
    }

    public BluetoothDevice getWorkingDevice() {
        return mBluetoothService.getWorkingDevice();
    }

    public boolean sendWhenReady(@NonNull Object object) {
        final String msg;
        if (object instanceof String) {
            msg = (String) object;
        }
        else if (object instanceof SignalingMessageItem){
            SignalingMessageItem signalingObject = (SignalingMessageItem) object;
            msg = new Gson().toJson(signalingObject);
        } else {
            return false;
        }

        Log.d(TAG, "sendWhenReady MSG: " + msg);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mBluetoothService.write(msg);
            }
        };
        return listActionToDone.offer(runnable) && continueActions();
    }

    @Override
    public void onConnectivityStateChanged(BTConnectivityService.ConnectionState state) {
        switch (state) {
            case STATE_DISCONNECTED:
                break;
            case STATE_CONNECTED:
            case STATE_LISTEN:
            case IDLE:
                continueActions();
                break;
        }
    }

    private void handleIncomingMsg (String msg) {
        try {
            SignalingMessageItem btMsg = new Gson()
                    .fromJson(msg, SignalingMessageItem.class);

            SignalingMessageItem.MessageType btMsgType = btMsg.getMessageType();
            Intent intent = null;
            if (btMsgType == SignalingMessageItem.MessageType.SDP_EXCHANGE) {
                if (btMsg.getWorkingSdp().type == SessionDescription.Type.OFFER) {
                    intent = new Intent(VoIPEngineService.ACTION_OFFER_SDP, Uri.EMPTY,
                            mLocalContext, VoIPEngineService.class);
                }
                if (btMsg.getWorkingSdp().type == SessionDescription.Type.ANSWER) {
                    intent = new Intent(VoIPEngineService.ACTION_ANSWER_SDP, Uri.EMPTY,
                            mLocalContext, VoIPEngineService.class);
                }
            }
            if (btMsgType == SignalingMessageItem.MessageType.CANDIDATES) {
                intent = new Intent(VoIPEngineService.ACTION_INCOMING_CANDIDATES, Uri.EMPTY,
                        mLocalContext, VoIPEngineService.class);
            }


            if (intent != null) {
                intent.putExtra(VoIPEngineService.EXTRA_SIGNAL_MSG, btMsg);
                mLocalContext.startService(intent);
            }
        }
        catch (JsonSyntaxException ex) {
            Log.e(TAG, "Wrong representation of Call Msg!", ex);
        }
    }


    private boolean continueActions () {
        Log.d(TAG, "continueActions() called");
        BTConnectivityService.ConnectionState state = mBluetoothService.getState();
        if (state == BTConnectivityService.ConnectionState.IDLE) {
            mBluetoothService.start();
            return false;
        }
        if (state != BTConnectivityService.ConnectionState.STATE_CONNECTED) {
            Log.e(TAG, "Device not connected. Waiting.");
            return false;
        }

        Log.d(TAG, "continueActions() ready to work");
        Runnable nextAction = listActionToDone.poll();
        if (nextAction == null) {
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
