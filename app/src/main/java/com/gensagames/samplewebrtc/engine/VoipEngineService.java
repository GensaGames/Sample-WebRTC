package com.gensagames.samplewebrtc.engine;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;
import com.gensagames.samplewebrtc.engine.utils.PairTuple;
import com.gensagames.samplewebrtc.model.BTDeviceItem;
import com.gensagames.samplewebrtc.model.RTCMessageItem;
import com.gensagames.samplewebrtc.signaling.BTSignalingObserver;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by GensaGames
 * GensaGames
 */

public class VoIPEngineService extends Service {

    private static final String TAG = VoIPEngineService.class.getSimpleName();

    public static final String ACTION_IDLE = "action.idle";
    public static final String ACTION_START_CALL = "action.start.call";
    public static final String ACTION_ANSWER_CALL = "action.receive.call";
    public static final String ACTION_OFFER_SDP = "action.offer.sdp";
    public static final String ACTION_ANSWER_SDP = "action.answer.sdp";

    public static final String EXTRA_DEVICE_ITEM = "extra.device.item";
    public static final String EXTRA_RTC_ITEM = "extra.rtc.msg";
    public static final String ANNOUNCE_INCOMING_CALL = "announce.incoming.call";

    private Handler mLocalHandler;
    private Map<Long, PairTuple<RTCSession, BTDeviceItem>> mSessionMap;
    private BTSignalingObserver mBtSignalingObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalHandler = new Handler();
        mSessionMap = new LinkedHashMap<>();
        mBtSignalingObserver = new BTSignalingObserver(getApplicationContext());

        VoIPRTCClient.getInstance().createPeerFactory(getApplicationContext(),
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
                startCall((BTDeviceItem) intent
                        .getSerializableExtra(EXTRA_DEVICE_ITEM));
                break;
            case ACTION_ANSWER_CALL:
                answerIncomingCall((RTCMessageItem) intent
                        .getSerializableExtra(EXTRA_RTC_ITEM));
                break;
            case ACTION_OFFER_SDP:
                notifyIncomingCall((RTCMessageItem) intent
                        .getSerializableExtra(EXTRA_RTC_ITEM));
                break;
            case ACTION_ANSWER_SDP:
                break;
            case ACTION_IDLE:
                break;
        }
        return START_STICKY ;
    }

    private void answerIncomingCall (final RTCMessageItem item) {
        VoIPRTCClient client = VoIPRTCClient.getInstance();
        if (!client.isCreated()) {
            Log.e(TAG, "PeerFactory not created!");
            return;
        }
        final BluetoothDevice device = mBtSignalingObserver.getWorkingDevice();
        if (!mBtSignalingObserver.isConnected() && device != null) {
            Log.e(TAG, "Bluetooth disconnected! Cannot answer call.");
            return;
        }

        client.createPeerConnection(new VoIPRTCClient.PeerCreationListener() {
            @Override
            public void onPeerCreated(RTCSession session) {
                long sessionId = session.getSessionId();
                mSessionMap.put(sessionId, new PairTuple<>(session, BTDeviceItem.
                        createFromBT(device, getString(R.string.name_unknown))));
                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.setRemoteDescription(item.getWorkingSdp());
                session.createAnswer();
            }
        }, null, null, null, null);
    }

    private void notifyIncomingCall (RTCMessageItem item) {
        Intent intent = new Intent(ANNOUNCE_INCOMING_CALL);
        intent.putExtra(EXTRA_RTC_ITEM, item);
        getApplicationContext().sendBroadcast(intent);
    }


    /**
     * Create connection for bluetooth, and just send raw data, as ping!
     * Started action for VoIPRTCClient to create PeerConnection.
     */
    private void startCall (@NonNull final BTDeviceItem device) {
        VoIPRTCClient client = VoIPRTCClient.getInstance();
        if (!client.isCreated()) {
            Log.e(TAG, "PeerFactory not created!");
            return;
        }
        mBtSignalingObserver.setWorkingAddress(device.getDeviceAddress());
        mBtSignalingObserver.sendWhenReady("");

        client.createPeerConnection(new VoIPRTCClient.PeerCreationListener() {
            @Override
            public void onPeerCreated(RTCSession session) {
                long sessionId = session.getSessionId();
                mSessionMap.put(sessionId, new PairTuple<>(session, device));
                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.createOffer();
            }
        }, null, null, null, null);
    }



    private class PeerEventsHandler implements RTCSession.PeerEventsListener {
        private long mSessionId;

        public PeerEventsHandler(long sessionId) {
            mSessionId = sessionId;
        }

        @Override
        public void onOfferDescriptionSet(SessionDescription sdp) {
            PairTuple<RTCSession, BTDeviceItem> tuple = mSessionMap.get(mSessionId);
            RTCMessageItem messageItem = new RTCMessageItem(tuple.getSecond().getDeviceName(),
                    tuple.getFirst().getSessionId(), RTCMessageItem.MessageType.SDP_EXCHANGE,
                    tuple.getFirst().getWorkingSdp(), null);
            mBtSignalingObserver.sendWhenReady(messageItem);
        }

        @Override
        public void onRemoteDescriptionSet(SessionDescription sdp) {

        }

        @Override
        public void onIceCandidate(IceCandidate candidate) {

        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {

        }

        @Override
        public void onIceConnected() {

        }

        @Override
        public void onIceDisconnected() {

        }

        @Override
        public void onPeerConnectionClosed() {

        }

        @Override
        public void onPeerConnectionStatsReady(StatsReport[] reports) {

        }

        @Override
        public void onPeerConnectionError(String description) {

        }
    }

}
