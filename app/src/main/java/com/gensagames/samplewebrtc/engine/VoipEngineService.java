package com.gensagames.samplewebrtc.engine;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.model.BtDeviceItem;
import com.gensagames.samplewebrtc.signaling.BtConnectivityDispatcher;

import java.util.Random;

/**
 * Created by GensaGames
 * GensaGames
 */

public class VoIPEngineService extends Service {

    private static final String TAG = VoIPEngineService.class.getSimpleName();

    public static final String ACTION_IDLE = "action.idle";
    public static final String ACTION_START_CALL = "action.startcall";
    public static final String EXTRA_START_CALL = "extra.startcall";

    private Handler mLocalHandler;
    private BtConnectivityDispatcher mBtConnectivityDispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalHandler = new Handler();
        mBtConnectivityDispatcher = new BtConnectivityDispatcher();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY ;
        }
        String action = intent.getAction();
        Log.i(TAG, "Proceed with action: " + action);
        switch (action) {
            case ACTION_START_CALL:
                startCall((BtDeviceItem) intent
                        .getSerializableExtra(EXTRA_START_CALL));
                break;
            case ACTION_IDLE:
                break;
        }
        return START_STICKY ;
    }

    private void startCall (@NonNull final BtDeviceItem device) {
        mBtConnectivityDispatcher.setWorkingAddress(device.getDeviceAddress());
        mLocalHandler.post(new Runnable() {
            @Override
            public void run() {
                mBtConnectivityDispatcher.sendWhenReady("Hello from device \""
                        + new Random().nextInt(20) + "\"");
                mLocalHandler.postDelayed(this, 10000);
            }
        });
    }




}
