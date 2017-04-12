package com.gensagames.samplewebrtc.engine;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;
import com.gensagames.samplewebrtc.model.CallSessionItem;
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

    public static final String EXTRA_SIGNAL_MSG = "extra.bt.msg";
    public static final String EXTRA_CALL_SESSION = "extra.call.session";

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

        RTCClient.getInstance().createPeerFactory(getApplicationContext(),
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
                startCall((CallSessionItem) intent
                        .getSerializableExtra(EXTRA_CALL_SESSION));
                break;
            case ACTION_ANSWER_CALL:
                answerIncomingCall((CallSessionItem) intent
                        .getSerializableExtra(EXTRA_CALL_SESSION));
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
        if (mSessionMap.isEmpty()) {
            return;
        }
        Long sessionId = mSessionMap.keySet().iterator().next();
        SessionInfoHolder holder = mSessionMap.get(sessionId);
        if (holder == null || holder.getSession() == null) {
            return;
        }
        holder.getSession().closeSession();
        mSessionMap.remove(sessionId);
        notifyCallDisconnected(null);
    }

    private synchronized void handleIncomingCandidates (SignalingMessageItem item) {
        SessionInfoHolder holder = mSessionMap.get(item.getPeerSessionId());
        if (item.getCandidates() == null) {
            Log.e(TAG, "Received Null IceCandidate!");
            return;
        }
        if (holder == null) {
            Log.e(TAG, "Received IceCandidate on empty Session!");
            return;
        }
        holder.addSignalingMsg(item);
        List<IceCandidate> list = holder.getRemoteIceCandidates();
        list.addAll(item.getCandidates());

        RTCSession session = holder.getSession();
        if (session != null) {
            session.setRemoteCandidates(list);
        }
    }

    private synchronized void handleIncomingCall(SignalingMessageItem signalingMsg) {
        final BluetoothDevice device = mBtSignalingObserver.getWorkingDevice();
        if (!mBtSignalingObserver.isConnected() && device == null) {
            Log.e(TAG, "Bluetooth disconnected! Cannot handle call.");
            return;
        }
        long sessionId = signalingMsg.getPeerSessionId();
        SessionInfoHolder holder = new SessionInfoHolder();
        mSessionMap.put(sessionId, holder);

        CallSessionItem item = new CallSessionItem(signalingMsg.getUserName(), sessionId,
                CallSessionItem.CallState.INCOMING);
        item.setBluetoothAddress(device.getAddress());

        holder.addSignalingMsg(signalingMsg);
        holder.setRemoteSdp(signalingMsg.getWorkingSdp());
        holder.setCallSession(item);

        notifyIncomingCall(item);
    }


    private synchronized void answerOutgoingCall(SignalingMessageItem item) {
        SessionInfoHolder holder = mSessionMap.get(item.getPeerSessionId());
        holder.addSignalingMsg(item);

        RTCSession session = holder.getSession();
        if (session == null) {
            Log.e(TAG, "Cannot find session!");
            return;
        }
        session.setRemoteDescription(item.getWorkingSdp());
    }

    /**
     * Received incoming SDP with Offer. Create PeerConnection and
     * proceed with answering SDP. Here we should UPDATE SESSION ID.
     * @param item - item from signaling
     */
    private synchronized void answerIncomingCall (final CallSessionItem item) {
        RTCClient client = RTCClient.getInstance();
        client.createPeerConnection(new RTCClient.PeerCreationListener() {
            @Override
            public void onPeerCreated(RTCSession session) {
                long sessionId = item.getSessionId();
                SessionInfoHolder holder = mSessionMap.get(sessionId);
                holder.setRTCSession(session);

                session.setSessionId(sessionId);
                session.setRemoteCandidates(holder.getRemoteIceCandidates());
                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.setRemoteDescription(holder.getRemoteSdp());
                session.createAnswer();
            }
        }, null, null, null, null);
    }

    /**
     * Create connection for bluetooth, and just send raw data, as ping!
     * Started action for RTCClient to create PeerConnection.
     * @param device - device to connect signaling
     */
    private synchronized void startCall (@NonNull final CallSessionItem item) {
        mBtSignalingObserver.connectAddress(item.getBluetoothAddress());
        RTCClient.getInstance().createPeerConnection(new RTCClient.PeerCreationListener() {
            @Override
            public void onPeerCreated(RTCSession session) {
                Log.d(TAG, "Received new Session...");
                SessionInfoHolder holder = new SessionInfoHolder();
                long sessionId = session.getSessionId();
                mSessionMap.put(sessionId, holder);

                item.setSessionId(sessionId);
                item.setConnectionState(CallSessionItem.CallState.CALLING);
                notifyOutgoingCall(item);

                holder.setCallSession(item);
                holder.setRTCSession(session);

                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.createOffer();
            }
        }, null, null, null, null);
    }

    /**
     * ************************************************************
     * Send notify action to Subscribers of this Service!
     */

    private void notifyIncomingCall (CallSessionItem item) {
        Log.d(TAG, "notifyIncomingCall... ");
        Intent intent = new Intent(NOTIFY_INCOMING_CALL);
        intent.putExtra(EXTRA_CALL_SESSION, item);
        getApplicationContext().sendBroadcast(intent);
    }

    private void notifyCallConnected (CallSessionItem item) {
        Log.d(TAG, "notifyCallConnected... ");
        Intent intent = new Intent(NOTIFY_CALL_CONNECTED);
        intent.putExtra(EXTRA_CALL_SESSION, item);
        getApplicationContext().sendBroadcast(intent);
    }

    private void notifyOutgoingCall (CallSessionItem item) {
        Log.d(TAG, "notifyOutgoingCall... ");
        Intent intent = new Intent(NOTIFY_OUTGOING_CALL);
        intent.putExtra(EXTRA_CALL_SESSION, item);
        getApplicationContext().sendBroadcast(intent);
    }

    private void notifyCallDisconnected (CallSessionItem item) {
        Log.d(TAG, "notifyCallDisconnected... ");
        Intent intent = new Intent(NOTIFY_CALL_DISCONNECTED);
        intent.putExtra(EXTRA_CALL_SESSION, item);
        getApplicationContext().sendBroadcast(intent);
    }

    /**
     * **********************************************************
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
            CallSessionItem item  = mSessionMap.get(mSessionId).getCallSessionItem();
            item.setConnectionState(CallSessionItem.CallState.CONNECTED);
            notifyCallConnected(item);

        }

        @Override
        public void onIceDisconnected() {
            Log.d(TAG, "onIceDisconnected");
            CallSessionItem item  = mSessionMap.get(mSessionId).getCallSessionItem();
            item.setConnectionState(CallSessionItem.CallState.DISCONNECTED);
            notifyCallConnected(item);
        }

        @Override
        public void onPeerConnectionStatsReady(StatsReport[] reports) {

        }

        private void signalingSdp (SessionDescription sdp) {
            SignalingMessageItem messageItem = new SignalingMessageItem(mSessionMap.get(mSessionId)
                    .getCallSessionItem().getRemoteName(), mSessionId, SignalingMessageItem
                    .MessageType.SDP_EXCHANGE, sdp, null);
            mBtSignalingObserver.sendWhenReady(messageItem);
        }

        private void signalingCandidate(IceCandidate candidate) {
            List<IceCandidate> candidates = new ArrayList<>();
            candidates.add(candidate);

            SignalingMessageItem messageItem = new SignalingMessageItem(mSessionMap.get(mSessionId)
                    .getCallSessionItem().getRemoteName(), mSessionId, SignalingMessageItem
                    .MessageType.CANDIDATES, candidates, null, null);
            mBtSignalingObserver.sendWhenReady(messageItem);
        }
    }

    private class SessionInfoHolder {

        private RTCSession session;
        private CallSessionItem callSessionItem;
        private SessionDescription remoteSdp;
        private List<IceCandidate> remoteIceCandidates = new LinkedList<>();

        public RTCSession getSession() {
            return session;
        }

        public void setRTCSession(RTCSession session) {
            this.session = session;
        }
        public void setCallSession(CallSessionItem item) {
            this.callSessionItem = item;
        }

        public SessionDescription getRemoteSdp() {
            return remoteSdp;
        }

        public void setRemoteSdp(SessionDescription remoteSdp) {
            this.remoteSdp = remoteSdp;
        }

        public List<IceCandidate> getRemoteIceCandidates() {
            return remoteIceCandidates;
        }
        public void addSignalingMsg(SignalingMessageItem signalingMsg) {

        }

        public CallSessionItem getCallSessionItem() {
            return callSessionItem;
        }
    }

    /**
     * ************************************************************
     * Some changes for sending Signaling message, with RTC Events.
     * USED JUST FOR SAMPLE, TO SHOW RENEGOTIATION
     */

}
