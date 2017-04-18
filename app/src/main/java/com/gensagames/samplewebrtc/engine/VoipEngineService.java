package com.gensagames.samplewebrtc.engine;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;
import com.gensagames.samplewebrtc.engine.utils.ProxyRenderer;
import com.gensagames.samplewebrtc.engine.utils.VideoCaptures;
import com.gensagames.samplewebrtc.model.CallSessionItem;
import com.gensagames.samplewebrtc.model.SignalingMessageItem;
import com.gensagames.samplewebrtc.signaling.BTSignalingObserver;
import com.gensagames.samplewebrtc.view.fragments.MainSliderFragment;

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
    public static final String EXTRA_LOCAL_RENDERER = "extra.local.renderer";
    public static final String EXTRA_REMOTE_RENDERER = "extra.remote.renderer";

    public static final String NOTIFY_INCOMING_CALL = "notify.incoming.call";
    public static final String NOTIFY_OUTGOING_CALL = "notify.outgoing.call";
    public static final String NOTIFY_CALL_CONNECTED = "notify.call.connected";
    public static final String NOTIFY_CALL_DISCONNECTED = "notify.call.disconnected";
    public static final String NOTIFY_SIGNAL_MSG = "notify.signaling.msg";


    private Handler mLocalUiHandler;
    private Map<Long, SessionInfoHolder> mSessionMap;
    private BTSignalingObserver mBtSignalingObserver;
    private PeerConnectionFactory.Options mPeerOptions;

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalUiHandler = new Handler(Looper.getMainLooper());
        mSessionMap = new LinkedHashMap<>();
        mBtSignalingObserver = new BTSignalingObserver(getApplicationContext());

        mPeerOptions = new PeerConnectionFactory.Options();
        RTCClient.getInstance().createPeerFactory(getApplicationContext(),
                mPeerOptions , PeerConnectionParameters.getDefaultVideo(), null);
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
                                .getSerializableExtra(EXTRA_CALL_SESSION),
                        (ProxyRenderer) intent
                                .getSerializableExtra(EXTRA_LOCAL_RENDERER),
                        (ProxyRenderer) intent
                                .getSerializableExtra(EXTRA_REMOTE_RENDERER));
                break;
            case ACTION_ANSWER_CALL:
                answerIncomingCall((CallSessionItem) intent
                        .getSerializableExtra(EXTRA_CALL_SESSION),
                        (ProxyRenderer) intent
                                .getSerializableExtra(EXTRA_LOCAL_RENDERER),
                        (ProxyRenderer) intent
                                .getSerializableExtra(EXTRA_REMOTE_RENDERER));
                break;
            case ACTION_OFFER_SDP:
                incomingCall((SignalingMessageItem) intent
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
     * TODO(Conference) Optimize this for Conference!
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
        if (item.getCandidates() == null) {
            Log.e(TAG, "Received Null IceCandidate!");
            return;
        }
        SessionInfoHolder holder = mSessionMap.get(item.getPeerSessionId());
        if (holder == null) {
            holder = new SessionInfoHolder();
            mSessionMap.put(item.getPeerSessionId(), holder);
        }
        holder.getRemoteIceCandidates().addAll(item.getCandidates());
        RTCSession session = holder.getSession();
        if (session != null) {
            session.setRemoteCandidates(item.getCandidates());
        }

        holder.addSignalingMsg(item);
    }

    /**
     * Just creation SessionInfoHolder and proceed with notification.
     * Nothing interesting.
     * @param signalingMsg - incoming msg
     */
    private synchronized void incomingCall(SignalingMessageItem signalingMsg) {
        final BluetoothDevice device = mBtSignalingObserver.getWorkingDevice();
        if (!mBtSignalingObserver.isConnected() && device == null) {
            Log.e(TAG, "Bluetooth disconnected! Cannot handle call.");
            return;
        }
        long sessionId = signalingMsg.getPeerSessionId();
        SessionInfoHolder holder = mSessionMap.get(sessionId);
        if (holder == null) {
            holder = new SessionInfoHolder();
            mSessionMap.put(sessionId, holder);
        }

        CallSessionItem item = new CallSessionItem(signalingMsg.getUserName(), sessionId,
                CallSessionItem.CallState.INCOMING);
        item.setBluetoothAddress(device.getAddress());

        holder.setRemoteSdp(signalingMsg.getWorkingSdp());
        holder.setCallSession(item);

        notifyIncomingCall(item);
        holder.addSignalingMsg(signalingMsg);
    }


    /**
     * Handle remote SDP Answer. (Our outgoing call was answered)
     * Need to save as Remote Description.
     * @param item -item to answer
     */
    private synchronized void answerOutgoingCall(SignalingMessageItem item) {
        SessionInfoHolder holder = mSessionMap.get(item.getPeerSessionId());

        RTCSession session = holder.getSession();
        if (session == null) {
            Log.e(TAG, "Cannot find session!");
            return;
        }
        session.setRemoteDescription(item.getWorkingSdp());
        holder.addSignalingMsg(item);
    }

    /**
     * Received incoming SDP with Offer. Create PeerConnection and
     * proceed with answering SDP. Here we should UPDATE SESSION ID.
     * @param item - item from signaling
     */
    private synchronized void answerIncomingCall (final CallSessionItem item,
                                                  @Nullable ProxyRenderer localRenderer,
                                                  @Nullable ProxyRenderer remoteRenderer) {
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
        }, VideoCaptures.createCorrectCapturer(getApplicationContext()), localRenderer, null);
    }

    /**
     * Create connection for bluetooth, and just send raw data, as ping!
     * Started action for RTCClient to create PeerConnection.
     * @param item - device to connect signaling
     */
    private synchronized void startCall (@NonNull final CallSessionItem item,
                                         @Nullable ProxyRenderer localRenderer,
                                         @Nullable ProxyRenderer remoteRenderer) {
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
        }, VideoCaptures.createCorrectCapturer(getApplicationContext()), localRenderer, null);
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
     * Some changes for sending Signaling message, with RTC Events.
     * USED JUST FOR SAMPLE, TO SHOW RENEGOTIATION
     */
    private void notifySignalingMsg (SignalingMessageItem item) {
        Log.d(TAG, "notifySignalingMsg... ");
        Intent intent = new Intent(NOTIFY_SIGNAL_MSG);
        intent.putExtra(EXTRA_SIGNAL_MSG, item);
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
            notifyCallDisconnected(item);
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

        private synchronized void signalingCandidate(IceCandidate candidate) {
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

        private RTCSession getSession() {
            return session;
        }

        private void setRTCSession(RTCSession session) {
            this.session = session;
        }

        private void setCallSession(CallSessionItem item) {
            this.callSessionItem = item;
        }

        private SessionDescription getRemoteSdp() {
            return remoteSdp;
        }

        private void setRemoteSdp(SessionDescription remoteSdp) {
            this.remoteSdp = remoteSdp;
        }

        private List<IceCandidate> getRemoteIceCandidates() {
            return remoteIceCandidates;
        }

        private CallSessionItem getCallSessionItem() {
            return callSessionItem;
        }


        private void addSignalingMsg(final SignalingMessageItem signalingMsg) {
            mLocalUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifySignalingMsg(signalingMsg);
                }
            });
        }

    }

}
