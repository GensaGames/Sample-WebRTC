package com.gensagames.samplewebrtc.engine;

import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.parameters.Configs;
import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;
import com.gensagames.samplewebrtc.engine.utils.SdpConfig;

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

public class RTCSession implements PeerConnection.Observer {

    private static final String TAG = RTCSession.class.getSimpleName();
    private PeerConnectionParameters peerConnectionParameters;
    private PeerConnection mPeerConnection;
    private PeerSdpObserver mSdpObserver;

    private DataChannel mDataChannel;
    private AudioTrack mAudioTrack;
    private VideoTrack mVideoTrack;

    private Executor mWorkingExecutor;
    private PeerConnection.IceConnectionState mIceConnectionState;
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
        void onOfferDescriptionSet(final SessionDescription sdp);

        void onRemoteDescriptionSet(final SessionDescription sdp);

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

    protected RTCSession() {
        mSdpObserver = new PeerSdpObserver();
        mSessionId = UUID.randomUUID().getMostSignificantBits();
        mWorkingExecutor = VoIPRTCClient.getInstance().getExecutor();
        peerConnectionParameters = VoIPRTCClient.getInstance()
                .getPeerConnectionParameters();
    }

    protected RTCSession configure(PeerConnection peerConnection, @Nullable DataChannel dataChannel,
                                   AudioTrack audioTrack, @Nullable VideoTrack videoTrack) {
        mPeerConnection = peerConnection;
        mDataChannel = dataChannel;
        mAudioTrack = audioTrack;
        mVideoTrack = videoTrack;
        return this;
    }

    public PeerConnection.IceConnectionState getIceConnectionState() {
        return mIceConnectionState;
    }

    public SessionDescription getWorkingSdp() {
        return mWorkingSdp;
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
                isInitiator = true;
                mPeerConnection.createOffer(mSdpObserver,
                        VoIPRTCClient.getInstance().getSdpConstraints());
            }
        });
    }


    public void createAnswer() {
        mWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                isInitiator = false;
                mPeerConnection.createAnswer(mSdpObserver,
                        VoIPRTCClient.getInstance().getSdpConstraints());
            }
        });
    }

    public void setRemoteDescription (final SessionDescription sessionDescription) {
        mWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "setRemoteDescription...");
                String sdpDescription = sessionDescription.description;

                if (peerConnectionParameters.videoCallEnabled) {
                    sdpDescription = preferCodec(sdpDescription,
                            peerConnectionParameters.videoCodec, false);
                }
                if (peerConnectionParameters.audioStartBitrate > 0) {
                    sdpDescription = SdpConfig.setStartBitrate(Configs.AUDIO_CODEC_OPUS,
                            false, sdpDescription, peerConnectionParameters.audioStartBitrate);
                }
                SessionDescription sdpRemote = new SessionDescription
                        (sessionDescription.type, sdpDescription);
                mPeerConnection.setRemoteDescription(mSdpObserver, sdpRemote);
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
        Log.d(TAG, "PeerConnection.SignalingState: " + signalingState);

    }

    @Override
    public void onIceConnectionChange(final PeerConnection.IceConnectionState iceConnectionState) {
        mWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "IceConnectionState: " + iceConnectionState);
                mIceConnectionState = iceConnectionState;
                switch (iceConnectionState) {
                    case CONNECTED:
                        mPeerEventsListener.onIceConnected();
                        break;
                    case FAILED:
                    case DISCONNECTED:
                        mPeerEventsListener.onIceDisconnected();
                        break;
                }
            }
        });
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG, "onIceConnectionReceivingChange: " + b);

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, "onIceGatheringChange: " + iceGatheringState);

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "onIceCandidate: " + iceCandidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d(TAG, "onIceCandidatesRemoved");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG, "onAddStream");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded");

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack");

    }


    private class PeerSdpObserver implements SdpObserver {
        /**
         * Applying custom notification here.
         * In this example, just for Video Codec.
         * @param sessionDescription
         */
        @Override
        public void onCreateSuccess(final SessionDescription sessionDescription) {
            mWorkingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onCreateSuccess SDP:\n" +  sessionDescription.description);
                    String sdpDescription = sessionDescription.description;
                    if (peerConnectionParameters.videoCallEnabled) {
                        sdpDescription = preferCodec(sdpDescription,
                                peerConnectionParameters.videoCodec, false);
                    }
                    mWorkingSdp = new SessionDescription
                            (sessionDescription.type, sdpDescription);

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
            mWorkingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (isInitiator) {
                        if (mPeerConnection.getRemoteDescription() == null) {
                            mPeerEventsListener.onOfferDescriptionSet(mWorkingSdp);
                        } else {
                            drainCandidates();
                        }
                    } else {
                        if (mPeerConnection.getLocalDescription() != null) {
                            mPeerEventsListener.onRemoteDescriptionSet(mWorkingSdp);
                            drainCandidates();
                        } else {
                            Log.d(TAG, "Remote SDP set successfully!");
                        }
                    }
                }
            });
            Log.d(TAG, "onSetSuccess");

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
