package com.gensagames.samplewebrtc.engine;

import android.app.Application;
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
import com.gensagames.samplewebrtc.view.MainActivity;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
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

public class VoIPEngineService {

    private static final String TAG = VoIPEngineService.class.getSimpleName();

    public static final String ACTION_IDLE = "action.idle";
    public static final String ACTION_START_CALL = "action.start.call";
    public static final String ACTION_ANSWER_CALL = "action.receive.call";
    public static final String ACTION_HANGUP_CALL = "action.hangup.call";

    private static final String ACTION_OFFER_SDP = "action.offer.sdp";
    private static final String ACTION_ANSWER_SDP = "action.answer.sdp";
    private static final String ACTION_INCOMING_CANDIDATES = "action.incoming.candidates";

    public interface VoIPEngineEvents {
        void onOutgoingCall (CallSessionItem item);
        void onIncomingCall (CallSessionItem item);
        void onSignalingMsg (SignalingMessageItem item);
        void onConnected (CallSessionItem item);
        void onDisconnected (CallSessionItem item);
    }

    private static VoIPEngineService instance;

    private Handler mLocalUiHandler;
    private Map<Long, SessionInfoHolder> mSessionMap;
    private List <VoIPEngineEvents> mEngineEventsList;
    private BTSignalingObserver mBtSignalingObserver;

    private VoIPEngineService() {
        onCreate();
    }

    public static VoIPEngineService getInstance() {
        if (instance == null) {
            synchronized (RTCClient.class) {
                if (instance == null) {
                    instance = new VoIPEngineService();
                }
            }
        }
        return instance;
    }

    public boolean removeEngineEventListener(VoIPEngineEvents events) {
        return mEngineEventsList.remove(events);
    }

    public boolean addEngineEventListener(VoIPEngineEvents events) {
        return !mEngineEventsList.contains(events)
                && mEngineEventsList.add(events);
    }

    public void onCreate() {
        mSessionMap = new LinkedHashMap<>();
        mEngineEventsList = new ArrayList<>();
        mLocalUiHandler = new Handler(Looper.getMainLooper());
        mBtSignalingObserver = new BTSignalingObserver(MainActivity.getContextInstance());

        PeerConnectionFactory.Options mPeerOptions = new PeerConnectionFactory.Options();
        RTCClient.getInstance().createPeerFactory(MainActivity.getContextInstance(),
                mPeerOptions, PeerConnectionParameters.getDefaultVideo(), null);
    }

    public void onStartCommand(CallSessionItem item) {
        switch (item.getAction()) {
            case ACTION_START_CALL:
                startCall(item);
                break;
            case ACTION_ANSWER_CALL:
                answerIncomingCall(item);
                break;
            case ACTION_HANGUP_CALL:
                hangupCall(item);
                break;
        }
    }

    public void onStartSignaling (SignalingMessageItem item) {
        switch (item.getAction()) {
            case ACTION_OFFER_SDP:
                incomingCall(item);
                break;
            case ACTION_ANSWER_SDP:
                answerOutgoingCall(item);
                break;
            case ACTION_INCOMING_CANDIDATES:
                handleIncomingCandidates(item);
                break;
        }
    }


    /**
     * Closing session including, which is not created yet
     */
    private synchronized void hangupCall (@NonNull CallSessionItem item) {
        long sessionId = item.getSessionId();
        SessionInfoHolder holder = mSessionMap.get(sessionId);

        if (holder == null || holder.getSession() == null) {
            Log.e(TAG, "Current Session empty!");
            return;
        }
        holder.getSession().closeSession();
        RTCClient.getInstance().cleanupMedia();
        mSessionMap.remove(sessionId);

        for (VoIPEngineEvents events : mEngineEventsList) {
            events.onDisconnected(item);
        }
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

        notifySignalingMsg(item);
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

        for (VoIPEngineEvents events : mEngineEventsList) {
            events.onIncomingCall(item);
        }
        notifySignalingMsg(signalingMsg);
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
        notifySignalingMsg(item);
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

                /**
                 * Session ID should be configured, to be the same, of both side.
                 * It will help, to resolve objects during signaling
                 */
                session.setSessionId(sessionId);
                session.setRemoteCandidates(holder.getRemoteIceCandidates());
                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.setRemoteDescription(holder.getRemoteSdp());
                session.createAnswer();
            }
        }, item.getLocalProxyRenderer(), item.getRemoteProxyRenderer());
    }

    /**
     * Create connection for bluetooth, and just send raw data, as ping!
     * Started action for RTCClient to create PeerConnection.
     * @param item - device to connect signaling
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

                for (VoIPEngineEvents events : mEngineEventsList) {
                    events.onOutgoingCall(item);
                }

                holder.setCallSession(item);
                holder.setRTCSession(session);

                session.setPeerEventsListener(new PeerEventsHandler(sessionId));
                session.createOffer();
            }
        }, item.getLocalProxyRenderer(), item.getRemoteProxyRenderer());
    }

    /**
     * This notify, just to show Signaling messages.
     * @param signalingMsg - Send events with current msg
     */
    private void notifySignalingMsg(final SignalingMessageItem signalingMsg) {
        for (VoIPEngineEvents events : mEngineEventsList) {
            events.onSignalingMsg(signalingMsg);
        }
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
        public void onIceConnected() {
            Log.d(TAG, "onIceConnected");
            notifyConnectionState(CallSessionItem.CallState.CONNECTED);
        }

        @Override
        public void onIceFailed() {
            Log.d(TAG, "onIceFailed");
            notifyConnectionState(CallSessionItem.CallState.DISCONNECTED);
        }

        @Override
        public void onIceDisconnected() {
            Log.d(TAG, "onIceDisconnected");
            notifyConnectionState(CallSessionItem.CallState.DISCONNECTED);
        }

        @Override
        public void onPeerConnectionStatsReady(StatsReport[] reports) {

        }

        private void notifyConnectionState (CallSessionItem.CallState state) {
            CallSessionItem item  = mSessionMap.get(mSessionId).getCallSessionItem();
            item.setConnectionState(state);

            if (state == CallSessionItem.CallState.DISCONNECTED) {
                item.setAction(ACTION_HANGUP_CALL);
                onStartCommand(item);
            }
            if (state == CallSessionItem.CallState.CONNECTED) {
                for (VoIPEngineEvents events : mEngineEventsList) {
                    events.onConnected(item);
                }
            }
        }

        private synchronized void signalingSdp (SessionDescription sdp) {
            SignalingMessageItem messageItem = new SignalingMessageItem(mSessionMap.get(mSessionId)
                    .getCallSessionItem().getRemoteName(), mSessionId, SignalingMessageItem
                    .MessageType.SDP_EXCHANGE, sdp, null);
            messageItem.setAction(sdp.type.equals(SessionDescription.Type.OFFER)
                    ? ACTION_OFFER_SDP : ACTION_ANSWER_SDP);
            mBtSignalingObserver.sendWhenReady(messageItem);
        }

        private synchronized void signalingCandidate(IceCandidate candidate) {
            List<IceCandidate> candidates = new ArrayList<>();
            candidates.add(candidate);

            SignalingMessageItem messageItem = new SignalingMessageItem(mSessionMap.get(mSessionId)
                    .getCallSessionItem().getRemoteName(), mSessionId, SignalingMessageItem
                    .MessageType.CANDIDATES, candidates, null, null);
            messageItem.setAction(ACTION_INCOMING_CANDIDATES);
            mBtSignalingObserver.sendWhenReady(messageItem);
        }
    }

    /**
     * **********************************************************
     * Main Holder, for gathering all RTC items, and Event handlers
     */

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

    }

}
