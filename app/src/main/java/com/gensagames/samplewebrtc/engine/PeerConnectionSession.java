package com.gensagames.samplewebrtc.engine;

import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;

import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoTrack;

import java.util.UUID;
import java.util.concurrent.Executor;

import static com.gensagames.samplewebrtc.engine.utils.SdpConfig.preferCodec;

/**
 * Created by GensaGames
 * GensaGames
 */

public class PeerConnectionSession implements PeerConnection.Observer {

    private static final String TAG = PeerConnectionSession.class.getSimpleName();
    private PeerConnectionParameters peerConnectionParameters;
    private PeerConnection mPeerConnection;
    private PeerSdpObserver mSdpObserver;

    private DataChannel mDataChannel;
    private AudioTrack mAudioTrack;
    private VideoTrack mVideoTrack;

    private Executor mWorkingExecutor;
    private PeerEventsListener mPeerEventsListener;
    private SessionDescription mWorkingSdp;
    private long mSessionId;
    private boolean isInitiator;

    /**
     * Peer connection events.
     */
    public interface PeerEventsListener {
        /**
         * Callback fired once local SDP is created and set.
         */
        void onLocalDescription(final SessionDescription sdp);

        /**
         * Callback fired once local Ice candidate is generated.
         */
        void onIceCandidate(final IceCandidate candidate);

        /**
         * Callback fired once local ICE candidates are removed.
         */
        void onIceCandidatesRemoved(final IceCandidate[] candidates);

        /**
         * Callback fired once connection is established (IceConnectionState is
         * CONNECTED).
         */
        void onIceConnected();

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * DISCONNECTED).
         */
        void onIceDisconnected();

        /**
         * Callback fired once peer connection is closed.
         */
        void onPeerConnectionClosed();

        /**
         * Callback fired once peer connection statistics is ready.
         */
        void onPeerConnectionStatsReady(final StatsReport[] reports);

        /**
         * Callback fired once peer connection error happened.
         */
        void onPeerConnectionError(final String description);
    }

    protected PeerConnectionSession () {
        mSdpObserver = new PeerSdpObserver();
        mSessionId = UUID.randomUUID().getMostSignificantBits();
        mWorkingExecutor = WebRTCClient.getInstance().getExecutor();
        peerConnectionParameters = WebRTCClient.getInstance()
                .getPeerConnectionParameters();
    }

    protected PeerConnectionSession configure(PeerConnection peerConnection, @Nullable DataChannel dataChannel,
                                              AudioTrack audioTrack, @Nullable VideoTrack videoTrack) {
        mPeerConnection = peerConnection;
        mDataChannel = dataChannel;
        mAudioTrack = audioTrack;
        mVideoTrack = videoTrack;
        return this;
    }

    public long getSessionId () {
        return mSessionId;
    }

    public void setPeerEventsListener (PeerEventsListener peerEventsListener) {
        this.mPeerEventsListener = peerEventsListener;
    }

    public void createOffer () {
        mWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mPeerConnection.createOffer(mSdpObserver,
                        WebRTCClient.getInstance().getSdpConstraints());
            }
        });
    }

    private void drainCandidates () {

    }
    /**
     * -------------------------------------------------------------------
     * -------------------------------------------------------------------
     */

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {

    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {

    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }


    private class PeerSdpObserver implements SdpObserver {
        /**
         * Applying custom notification here.
         * In this example, just for Video Codec.
         * @param sessionDescription
         */
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(TAG, "onCreateSuccess SDP: " + sessionDescription.description);
            String sdpDescription = sessionDescription.description;
            if (peerConnectionParameters.videoCallEnabled) {
                sdpDescription = preferCodec(sdpDescription,
                        peerConnectionParameters.videoCodec, false);
            }
            mWorkingSdp = new SessionDescription
                    (sessionDescription.type, sdpDescription);
            mWorkingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mPeerConnection.setLocalDescription
                            (PeerSdpObserver.this, mWorkingSdp);
                }
            });
        }
        /**
         * For offering peer connection we first create offer and set
         * local SDP, then after receiving answer set remote SDP.
         */
        @Override
        public void onSetSuccess() {
            Log.d(TAG, "onSetSuccess");
            if (isInitiator) {
                if (mPeerConnection.getRemoteDescription() == null) {
                    mPeerEventsListener.onLocalDescription(mWorkingSdp);
                } else {
                    drainCandidates();
                }
            } else {
                if (mPeerConnection.getLocalDescription() != null) {
                    mPeerEventsListener.onLocalDescription(mWorkingSdp);
                    drainCandidates();
                } else {
                    Log.d(TAG, "Remote SDP set succesfully. " +
                            "Should work now!");
                }
            }
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "onCreateFailure: " + s);

        }

        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, "onSetFailure: " + s);

        }
    }
}
