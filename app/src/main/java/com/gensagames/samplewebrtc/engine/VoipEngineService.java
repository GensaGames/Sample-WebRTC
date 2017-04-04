package com.gensagames.samplewebrtc.engine;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.signaling.BluetoothConnectivityService;
import com.gensagames.samplewebrtc.signaling.helper.OnMessageObservable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class VoIPEngineService extends Service {

    private static final String TAG = VoIPEngineService.class.getSimpleName();

    public static final String ACTION_IDLE = "action.idle";
    public static final String ACTION_START_CALL = "action.startcall";
    public static final String EXTRA_START_CALL = "extra.startcall";

    private BluetoothConnectivityService mBluetoothService;
    private SignalingHandler mSignalingHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mSignalingHandler = new SignalingHandler();
        mBluetoothService = new BluetoothConnectivityService(mSignalingHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY ;
        }
        String action = intent.getAction();
        Log.i(TAG, "Starting with action: " + action);
        switch (action) {
            case ACTION_START_CALL:
                startCall((BluetoothDeviceItem) intent
                        .getParcelableExtra(EXTRA_START_CALL));
                break;
            case ACTION_IDLE:
                break;
        }
        return START_STICKY ;
    }

    private void startCall (@NonNull BluetoothDeviceItem device) {

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    private class SignalingHandler implements OnMessageObservable {

        @Override
        public void onReceiveMsg(byte[] msgBytes, int length) {

        }

        @Override
        public void onSentMsg(byte[] msgBytes) {

        }
    }
}
