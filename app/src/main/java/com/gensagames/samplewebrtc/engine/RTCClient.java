package com.gensagames.samplewebrtc.engine;

import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;
import com.gensagames.samplewebrtc.engine.utils.VideoCaptures;
import com.gensagames.samplewebrtc.view.MainActivity;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.gensagames.samplewebrtc.engine.parameters.Configs.*;

/**
 * Created by GensaGames
 * GensaGames
 */

public class RTCClient implements WebRtcAudioRecord.WebRtcAudioRecordErrorCallback {

    /**
     * Just tracing values, for logs and other info
     */
    private static final String TAG = RTCClient.class.getSimpleName();
    private static final String NATIVE_TRACE_USE = "logcat:";
    private static final String FILE_TRACE_NAME = "app-webrtc.txt";
    private static final String FILE_AUDIO_DUMP = "audio.aecdump";

    private static RTCClient sClientInstance;
    private static Executor sWorkingExecutor;

    private PeerConnectionFactory.Options mPeerFactoryOptions;
    private PeerConnectionParameters mPeerConnectionParameters;
    private PeerConnectionFactory mPeerFactory;
    private EglBase mWorkingEglBase;

    private MediaConstraints mPeerConstraints;
    private MediaConstraints mAudioConstraints;
    private MediaConstraints mSdpMediaConstraints;

    private ParcelFileDescriptor mAecDumpFileDescriptor;
    private VideoCapturer mVideoCapturer;
    private MediaStream mMediaStream;
    private AudioSource mAudioSource;
    private VideoSource mVideoSource;

    public interface FactoryCreationListener {
        void onCreationDone (boolean isSuccessful);
    }

    public interface PeerCreationListener {
        void onPeerCreated(RTCSession session);
    }

    private RTCClient() {
        sWorkingExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public static RTCClient getInstance() {
        if (sClientInstance == null) {
            synchronized (RTCClient.class) {
                if (sClientInstance == null) {
                    sClientInstance = new RTCClient();
                }
            }
        }
        return sClientInstance;
    }

    public Executor getExecutor () {
        return sWorkingExecutor;
    }

    public EglBase getWorkingEglBase() {
        return mWorkingEglBase;
    }

    public MediaStream getLocalMediaStream () {
        return mMediaStream;
    }

    public PeerConnectionParameters getPeerConnectionParameters () {
        return mPeerConnectionParameters;
    }

    public MediaConstraints getSdpConstraints() {
        return mSdpMediaConstraints;
    }

    public void createPeerFactory(@NonNull final Context context,
                                  @NonNull PeerConnectionFactory.Options options,
                                  @NonNull PeerConnectionParameters peerConnectionParameters,
                                  @Nullable final FactoryCreationListener factoryCreationListener) {
        this.mPeerConnectionParameters = peerConnectionParameters;
        this.mPeerFactoryOptions = options;
        sWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                createPeerFactoryInternal(context, factoryCreationListener);
            }
        });
    }


    public void createPeerConnection (@NonNull final PeerCreationListener peerCreationListener,
                                      @Nullable final VideoRenderer.Callbacks videoCallbackLocal,
                                      @Nullable final VideoRenderer.Callbacks videoCallbacksRemote) {
        sWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                createPeerConnectionInternal(peerCreationListener,
                        videoCallbackLocal, videoCallbacksRemote);
            }
        });
    }

    /**
     * Used just for testing. Remove after some
     * Media cleaning up.
     */
    public void cleanupMedia () {
        sWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                cleanupMediaInternal();
            }
        });
    }

    /**
     * For creation PeerConnection with Video enable, we should also use
     * EglBase.Context renderEGLContext for PeerConnectionFactory
     *
     * PeerConnectionFactory.setVideoHwAccelerationOptions
     * (renderEGLContext, renderEGLContext);
     */
    private synchronized void createPeerFactoryInternal(Context context,
                                           FactoryCreationListener factoryCreationListener) {
        PeerConnectionFactory.initializeInternalTracer();
        if (mPeerConnectionParameters.tracing) {
            PeerConnectionFactory.startInternalTracingCapture(
                    Environment.getExternalStorageDirectory().getAbsolutePath()
                            + File.separator + FILE_TRACE_NAME);
        }
        Log.d(TAG, "Create peer connection factory. Use video: "
                + mPeerConnectionParameters.videoCallEnabled);

        StringBuilder fieldTrialsBuilder = new StringBuilder();
        if (mPeerConnectionParameters.videoFlexfecEnabled) {
            fieldTrialsBuilder.append(VIDEO_FLEXFEC_FIELDTRIAL);
            Log.d(TAG, "Enable FlexFEC field trial.");
        }
        fieldTrialsBuilder.append(VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL);
        PeerConnectionFactory.initializeFieldTrials(fieldTrialsBuilder.toString());
        Log.d(TAG, "Field trials: " + fieldTrialsBuilder.toString());


        Log.d(TAG, "Option useOpenSLES: " + mPeerConnectionParameters.useOpenSLES);
        if (!mPeerConnectionParameters.useOpenSLES) {
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
        } else {
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
        }

        Log.d(TAG, "Option disableBuiltInAEC: " + mPeerConnectionParameters.disableBuiltInAEC);
        if (mPeerConnectionParameters.disableBuiltInAEC) {
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        } else {
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
        }


        Log.d(TAG, "Option disableBuiltInNS: " + mPeerConnectionParameters.disableBuiltInNS);
        if (mPeerConnectionParameters.disableBuiltInNS) {
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        } else {
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
        }

        WebRtcAudioRecord.setErrorCallback(RTCClient.this);
        if (PeerConnectionFactory.initializeAndroidGlobals(
                context, true, false, mPeerConnectionParameters.videoCodecHwAcceleration)) {
            mPeerFactory = new PeerConnectionFactory(mPeerFactoryOptions);
        } else {
            Log.e(TAG, "Failed to initializeAndroidGlobals");
        }
        /*
        * Set default WebRTC tracing and INFO libjingle logging.
        * NOTE: this _must_ happen while |factory| is alive!
        */
        createMediaConstraints();
        boolean isSuccessful = mPeerFactory != null;
        if (isSuccessful && mPeerConnectionParameters.tracing) {
            Logging.enableTracing(NATIVE_TRACE_USE, EnumSet.
                    of(Logging.TraceLevel.TRACE_DEFAULT));
            Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);
        }

        if (factoryCreationListener != null) {
            factoryCreationListener.onCreationDone(isSuccessful);
        }
    }

    private synchronized void createMediaConstraints() {
        mPeerConstraints = new MediaConstraints();
        mPeerConstraints.optional.add(new MediaConstraints.
                KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));

        /* Create Audio constraints Added for audio performance measurements.
         * By default there are enabled!
         */
        mAudioConstraints = new MediaConstraints();
        if (mPeerConnectionParameters.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing");
            mAudioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
            mAudioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
            mAudioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
            mAudioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
        }
        if (mPeerConnectionParameters.enableLevelControl) {
            Log.d(TAG, "Enabling level control.");
            mAudioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_LEVEL_CONTROL_CONSTRAINT, "true"));
        }
        /* Create SDP constraints.
         */
        mSdpMediaConstraints = new MediaConstraints();
        mSdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        if (mPeerConnectionParameters.videoCallEnabled) {
            mSdpMediaConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        } else {
            mSdpMediaConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        }
    }

    /**
     * For creation PeerConnection with Video enable, we should also use
     * EglBase.Context renderEGLContext for PeerConnectionFactory
     */
    private synchronized void createPeerConnectionInternal(@NonNull PeerCreationListener peerCreationListener,
                                      @Nullable VideoRenderer.Callbacks videoLocalRenderer,
                                      @Nullable VideoRenderer.Callbacks videoRemoteRenderer) {
        if (mPeerFactory == null) {
            Log.e(TAG, "PeerConnection factory is not created");
            return;
        }
        RTCSession rtcSession = new RTCSession();
        PeerConnection peerConnection;
        DataChannel dataChannel = null;
        AudioTrack audioTrack = null;
        VideoTrack videoTrack = null;

        if (mPeerConnectionParameters.videoCallEnabled && mWorkingEglBase == null) {
            mWorkingEglBase = EglBase.create();
            EglBase.Context context = mWorkingEglBase.getEglBaseContext();
            mPeerFactory.setVideoHwAccelerationOptions(context, context);
        }
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(mPeerConnectionParameters.iceServers);
        /* - TCP candidates are only useful when connecting
         *   to a server that supports ICE-TCP.
         * - Use ECDSA encryption.
         */
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection
                .ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        Log.d(TAG, "PCConstraints: " + mPeerConstraints.toString());
        peerConnection = mPeerFactory.createPeerConnection(rtcConfig,
                mPeerConstraints, rtcSession);

        Log.d(TAG, "Created PeerConnection.");
        if (mPeerConnectionParameters.dataChannelParameters != null) {
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = mPeerConnectionParameters.dataChannelParameters.ordered;
            init.negotiated = mPeerConnectionParameters.dataChannelParameters.negotiated;
            init.maxRetransmits = mPeerConnectionParameters.dataChannelParameters.maxRetransmits;
            init.maxRetransmitTimeMs = mPeerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs;
            init.id = mPeerConnectionParameters.dataChannelParameters.id;
            init.protocol = mPeerConnectionParameters.dataChannelParameters.protocol;
            dataChannel = peerConnection.createDataChannel(getRandomLongUuid(), init);
            Log.d(TAG, "Created PeerConnection - DataChannel!");
        }
        /*
         * Checking Media Sources (Audio and Video), and
         * create/add them to the main MediaStream
         */
        Log.d(TAG, "Check and create Media...");
        createMediaStream();
        audioTrack = createAudioTrack();
        if (mPeerConnectionParameters.videoCallEnabled
                && videoLocalRenderer != null) {
            videoTrack = createVideoTrack(videoLocalRenderer);
        }
        peerConnection.addStream(mMediaStream);

        if (mPeerConnectionParameters.aecDump) {
            try {
                mAecDumpFileDescriptor = ParcelFileDescriptor.open(new File(Environment
                        .getExternalStorageDirectory().getPath() + File.separator + FILE_AUDIO_DUMP),
                                ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
                                        | ParcelFileDescriptor.MODE_TRUNCATE);
                mPeerFactory.startAecDump(mAecDumpFileDescriptor.getFd(), -1);
            } catch (IOException e) {
                Log.e(TAG, "Can not open aecdump file!", e);
            }
        }
        peerCreationListener.onPeerCreated(rtcSession.configure(peerConnection, dataChannel, null,
                audioTrack, videoTrack, videoLocalRenderer, videoRemoteRenderer));
    }


    private synchronized void createMediaStream() {
        if (mMediaStream == null) {
            mMediaStream = mPeerFactory.createLocalMediaStream(getRandomLongUuid());
        }
        if (mAudioSource == null) {
            mAudioSource = mPeerFactory.createAudioSource(mAudioConstraints);
        }

        if (mPeerConnectionParameters.videoCallEnabled) {
            if (mVideoCapturer == null) {
                mVideoCapturer = VideoCaptures.createCorrectCapturer
                        (MainActivity.getContextInstance());
            }
            if (mVideoSource == null) {
                mVideoSource = mPeerFactory.createVideoSource(mVideoCapturer);
            }
            mVideoCapturer.startCapture(mPeerConnectionParameters.videoWidth,
                    mPeerConnectionParameters.videoHeight, mPeerConnectionParameters.videoFps);
        }
    }

    private synchronized void cleanupMediaInternal () {
        try {
            if (mAudioSource != null) {
                mAudioSource.dispose();
                mAudioSource = null;
            }
            if (mVideoSource != null) {
                mVideoSource.dispose();
                mVideoSource = null;
            }
            if (mVideoCapturer != null) {
                mVideoCapturer.stopCapture();
                mVideoCapturer.dispose();
                mVideoCapturer = null;
            }
            if (mWorkingEglBase != null) {
                mWorkingEglBase.release();
                mWorkingEglBase = null;
            }

        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping capturer!", e);
        }
    }

    private VideoTrack createVideoTrack (@NonNull VideoRenderer.Callbacks videoCallbackLocal) {
        VideoTrack videoTrack = mPeerFactory.createVideoTrack
                (getRandomLongUuid(), mVideoSource);
        videoTrack.addRenderer(new VideoRenderer(videoCallbackLocal));
        videoTrack.setEnabled(true);
        mMediaStream.addTrack(videoTrack);
        return videoTrack;
    }

    private AudioTrack createAudioTrack () {
        AudioTrack audioTrack = mPeerFactory.createAudioTrack
                (getRandomLongUuid(), mAudioSource);
        audioTrack.setEnabled(true);
        mMediaStream.addTrack(audioTrack);
        return audioTrack;
    }

    private String getRandomLongUuid () {
        return String.valueOf(UUID.randomUUID().getMostSignificantBits()
                & Long.MAX_VALUE);
    }



    /*
     * -------------------------------------------------------------------
     * -------------------------------------------------------------------
     */

    /**
     * Special behavior of indicates error.
     * For most cases still unused
     */

    @Override
    public void onWebRtcAudioRecordInitError(String errorMessage) {
        Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
    }

    @Override
    public void onWebRtcAudioRecordStartError(String errorMessage) {
        Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorMessage);
    }

    @Override
    public void onWebRtcAudioRecordError(String errorMessage) {
        Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
    }
}
