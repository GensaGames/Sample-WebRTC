package com.gensagames.samplewebrtc.engine;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;
import com.gensagames.samplewebrtc.model.BtDeviceItem;
import com.gensagames.samplewebrtc.signaling.BtConnectivityDispatcher;

import org.webrtc.PeerConnectionFactory;

/**
 * Created by GensaGames
 * GensaGames
 */

public class VoIPEngineService extends Service implements WebRTCClient.OnPeerCreationListener {

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

        WebRTCClient.getInstance().createPeerFactory(getApplicationContext(),
                new PeerConnectionFactory.Options(), PeerConnectionParameters
                        .getDefaultAudioOnly(), null);
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

    private void startWork () {
        WebRTCClient client = WebRTCClient.getInstance();
        if (client.isCreated()) {
            client.createPeerConnection(this);
        }
    }

    private void startCall (@NonNull final BtDeviceItem device) {
        WebRTCClient client = WebRTCClient.getInstance();
        if (!client.isCreated()) {
            Log.e(TAG, "PeerFactory not created!");
            return;
        }
        /*  Create connection, and just send raw data, as ping!
         */
        mBtConnectivityDispatcher.setWorkingAddress(device.getDeviceAddress());
        mBtConnectivityDispatcher.sendWhenReady("");
        client.createPeerConnection(this);
    }


    @Override
    public void onPeerConnectionCreated(PeerConnectionSession session) {

    }
}
