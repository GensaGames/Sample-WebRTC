package com.gensagames.samplewebrtc.engine;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.model.SignalingMessageItem;
import com.gensagames.samplewebrtc.signaling.BTSignalingObserver;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public static final String ACTION_INCOMING_CANDIDATES = "action.incoming.candidates";
    public static final String ACTION_HANGUP_CALL = "action.hangup.call";

    public static final String EXTRA_DEVICE_ITEM = "extra.device.item";
    public static final String EXTRA_SIGNAL_MSG = "extra.bt.msg";

    public static final String NOTIFY_INCOMING_CALL = "notify.incoming.call";
    public static final String NOTIFY_OUTGOING_CALL = "notify.outgoing.call";
    public static final String NOTIFY_CALL_CONNECTED = "notify.call.connected";
    public static final String NOTIFY_CALL_DISCONNECTED = "notify.call.disconnected";

    private Map<Long, SessionInfoHolder> mSessionMap;
    private BTSignalingObserver mBtSignalingObserver;

    @Override
    public void onCreate() {
        super.onCreate();
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
                startCall((BluetoothDeviceItem) intent
                        .getSerializableExtra(EXTRA_DEVICE_ITEM));
                break;
            case ACTION_ANSWER_CALL:
                answerIncomingCall((SignalingMessageItem) intent
                        .getSerializableExtra(EXTRA_SIGNAL_MSG));
                break;
            case ACTION_OFFER_SDP:
                handleIncomingCall((SignalingMessageItem) intent
                        .getSerializableExtra(EXTRA_SIGNAL_MSG));
                break;
            case ACTION_ANSWER_SDP:
                answerOutgoingCall((SignalingMessageItem) intent
                        .getSerializableExtra(EXTRA_SIGNAL_MSG));
            case ACTION_INCOMING_CANDIDATES:
                handleIncomingCandidates((SignalingMessageItem) intent
                        .getSerializableExtra(EXTRA_SIGNAL_MSG));
                break;
            case ACTION_HANGUP_CALL:
                hangupCall(intent.getSerializableExtra(EXTRA_SIGNAL_MSG));
                break;
        }
        return START_STICKY ;
    }


    /**
     * Closing session including, which is not created yet
     * TODO(Items) Optimize objects to renegotiate!
     */
    private synchronized void hangupCall (Object item) {
        SessionInfoHolder holder = null;
        if (item instanceof BluetoothDeviceItem) {
            for (SessionInfoHolder infoHolder : mSessionMap.values()) {
                if (infoHolder.getDeviceItem().equals(item)) {
                    holder = infoHolder;
                }
            }
        }
        if (item instanceof SignalingMessageItem) {
            holder = mSessionMap.get(((SignalingMessageItem) item).getPeerSessionId());
        }
        if (holder == null) {
            return;
        }
        RTCSession session = holder.getSession();
        holder.setSession(null);
        if (session != null) {
            session.closeSession();
        }
    }

    private synchronized void handleIncomingCandidates (SignalingMessageItem item) {
        SessionInfoHolder holder = mSessionMap.get(item.getPeerSessionId());
        if (holder != null) {
            holder.getMessageItems().add(item);
            List<IceCandidate> list = holder.getRemoteIceCandidates();
            list.addAll(item.getCandidates());
            holder.getSession().setRemoteCandidates(list);
        } else {
            Log.e(TAG, "Received IceCandidate on empty Session!");
        }
    }

    private synchronized void handleIncomingCall(SignalingMessageItem item) {
        final BluetoothDevice device = mBtSignalingObserver.getWorkingDevice();
        if (!mBtSignalingObserver.isConnected() && device != null) {
            Log.e(TAG, "Bluetooth disconnected! Cannot handle call.");
            return;
        }
        long sessionId = item.getPeerSessionId();
        SessionInfoHolder holder = new SessionInfoHolder();
        holder.getMessageItems().add(item);
        holder.setDeviceItem(BluetoothDeviceItem.createFromBT
                (device, getString(R.string.name_unknown)));
        mSessionMap.put(sessionId, holder);

        notifyIncomingCall(item);
    }


    private synchronized void answerOutgoingCall(SignalingMessageItem item) {
        SessionInfoHolder holder = mSessionMap.get(item.getPeerSessionId());
        holder.getMessageItems().add(item);

        RTCSession session = holder.getSession();
        if (session == null) {
            Log.e(TAG, "Cannot find session!");
            return;
        }
        session.setRemoteDescription(item.getWorkingSdp());
        Log.d(TAG, "SHOULD WORK NOW!!!!");
    }

    /**
     * Received incoming SDP with Offer. Create PeerConnection and
     * proceed with answering SDP.
     * @param item - item from signaling
     */
    private synchronized void answerIncomingCall (final SignalingMessageItem item) {
        VoIPRTCClient client = VoIPRTCClient.getInstance();
        if (!client.isCreated()) {
            Log.e(TAG, "PeerFactory not created!");
            return;
        }

        client.createPeerConnection(new VoIPRTCClient.PeerCreationListener() {
            @Override
            public void onPeerCreated(RTCSession session) {
                long sessionId = item.getPeerSessionId();
                SessionInfoHolder holder = mSessionMap.get(sessionId);
                holder.getMessageItems().add(item);
                holder.setSession(session);

                session.setSessionId(sessionId);
                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.setRemoteDescription(item.getWorkingSdp());
                session.createAnswer();
            }
        }, null, null, null, null);
    }

    /**
     * Create connection for bluetooth, and just send raw data, as ping!
     * Started action for VoIPRTCClient to create PeerConnection.
     * @param device - device to connect signaling
     */
    private synchronized void startCall (@NonNull final BluetoothDeviceItem device) {
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
                SessionInfoHolder holder = new SessionInfoHolder();
                holder.setSession(session);
                holder.setDeviceItem(device);
                mSessionMap.put(sessionId, holder);

                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.createOffer();
                notifyOutgoingCall(device);
            }
        }, null, null, null, null);
    }

    /**
     * ************************************************************
     * Send notify action to Subscribers of this Service!
     */

    private void notifyIncomingCall (SignalingMessageItem item) {
        Intent intent = new Intent(NOTIFY_INCOMING_CALL);
        intent.putExtra(EXTRA_SIGNAL_MSG, item);
        getApplicationContext().sendBroadcast(intent);
    }

    private void notifyCallConnected (SignalingMessageItem item) {
        Intent intent = new Intent(NOTIFY_CALL_CONNECTED);
        intent.putExtra(EXTRA_SIGNAL_MSG, item);
        getApplicationContext().sendBroadcast(intent);
    }

    private void notifyOutgoingCall (BluetoothDeviceItem item) {
        Intent intent = new Intent(NOTIFY_OUTGOING_CALL);
        intent.putExtra(EXTRA_DEVICE_ITEM, item);
        getApplicationContext().sendBroadcast(intent);
    }

    private void notifyCallDisconnected (SignalingMessageItem item) {
        Intent intent = new Intent(NOTIFY_CALL_DISCONNECTED);
        intent.putExtra(EXTRA_SIGNAL_MSG, item);
        getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Main Handler for all PeerConnection Events.
     * Control specific behavior here.
     */

    private class PeerEventsHandler implements RTCSession.PeerEventsListener {
        private long mSessionId;

        public PeerEventsHandler(long sessionId) {
            mSessionId = sessionId;
        }

        @Override
        public void onLocalSdpForOffer(SessionDescription sdp) {
            Log.d(TAG, "onLocalSdpForOffer -  Signaling SDP");
            signalingSdp(sdp);
        }

        @Override
        public void onLocalSdpForRemote(SessionDescription sdp) {
            Log.d(TAG, "onLocalSdpForRemote -  Signaling SDP");
            signalingSdp(sdp);
        }

        @Override
        public void onIceCandidate(IceCandidate candidate) {
            Log.d(TAG, "onIceCandidate -  Signaling CANDIDATE");
            signalingCandidate(candidate);
        }


        @Override
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {
            Log.d(TAG, "onIceCandidatesRemoved");
        }
        /**
         * TODO(Items) Improve handling Item types
         * During sending updates about session
         */
        @Override
        public void onIceConnected() {
            Log.d(TAG, "onIceConnected");
            List<SignalingMessageItem> list = mSessionMap.
                    get(mSessionId).getMessageItems();
            if (list != null && !list.isEmpty()) {
                notifyCallConnected(list.get(list.size() - 1));
            }
        }

        @Override
        public void onIceDisconnected() {
            Log.d(TAG, "onIceDisconnected");
            List<SignalingMessageItem> list = mSessionMap.
                    get(mSessionId).getMessageItems();
            if (list != null && !list.isEmpty()) {
                notifyCallDisconnected(list.get(list.size() - 1));
            }
        }

        @Override
        public void onPeerConnectionStatsReady(StatsReport[] reports) {

        }


        private void signalingSdp (SessionDescription sdp) {
            SignalingMessageItem messageItem = new SignalingMessageItem(mSessionMap.get(mSessionId).getDeviceItem()
                    .getDeviceName(), mSessionId, SignalingMessageItem.MessageType.SDP_EXCHANGE, sdp, null);
            mBtSignalingObserver.sendWhenReady(messageItem);
        }


        private void signalingCandidate(IceCandidate candidate) {
            List<IceCandidate> candidates = new ArrayList<>();
            candidates.add(candidate);

            SignalingMessageItem messageItem = new SignalingMessageItem(mSessionMap.get(mSessionId).getDeviceItem().
                    getDeviceName(), mSessionId, SignalingMessageItem.MessageType.CANDIDATES,
                    candidates, null, null);
            mBtSignalingObserver.sendWhenReady(messageItem);
        }
    }

    private class SessionInfoHolder {
        private RTCSession session;
        private BluetoothDeviceItem deviceItem;
        private List<SignalingMessageItem> messageItems = new ArrayList<>();
        private List<IceCandidate> remoteIceCandidates = new LinkedList<>();

        public RTCSession getSession() {
            return session;
        }

        public void setSession(RTCSession session) {
            this.session = session;
        }

        public BluetoothDeviceItem getDeviceItem() {
            return deviceItem;
        }

        public void setDeviceItem(BluetoothDeviceItem deviceItem) {
            this.deviceItem = deviceItem;
        }

        public List<IceCandidate> getRemoteIceCandidates() {
            return remoteIceCandidates;
        }

        public List<SignalingMessageItem> getMessageItems() {
            return messageItems;
        }
    }

    /**
     * ************************************************************
     * Some changes for sending Signaling message, with RTC Events.
     */

}
