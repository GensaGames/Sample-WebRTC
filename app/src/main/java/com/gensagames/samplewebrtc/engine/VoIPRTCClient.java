package com.gensagames.samplewebrtc.engine;

import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gensagames.samplewebrtc.engine.parameters.PeerConnectionParameters;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpSender;
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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.gensagames.samplewebrtc.engine.parameters.Configs.*;

/**
 * Created by GensaGames
 * GensaGames
 */

public class VoIPRTCClient implements WebRtcAudioRecord.WebRtcAudioRecordErrorCallback {

    private static final String TAG = VoIPRTCClient.class.getSimpleName();
    private static final String NATIVE_TRACE_USE = "logcat:";
    private static final String FILE_TRACE_NAME = "app-webrtc.txt";
    private static final String FILE_AUDIO_DUMP = "audio.aecdump";

    /*
     * This might be optimized in generic way and conference supporting.
     * But for sample we can leave it.
     */
    private static final String STREAM_UID = "App-Audio-Stream";
    private static final String DATA_CHANNEL_UID = "App-Data-Channel";

    private static VoIPRTCClient instance;

    private Executor mWorkingExecutor;
    private PeerConnectionFactory.Options mPeerFactoryOptions;
    private PeerConnectionParameters mPeerConnectionParameters;
    private PeerConnectionFactory mPeerFactory;

    private MediaConstraints mPeerConstraints;
    private MediaConstraints mAudioConstraints;
    private MediaConstraints mSdpMediaConstraints;

    private ParcelFileDescriptor mAecDumpFileDescriptor;
    private MediaStream mMediaStream;
    private AudioSource mAudioSource;
    private VideoSource mVideoSource;

    public interface FactoryCreationListener {
        void onCreationDone (boolean isSuccessful);
    }

    public interface PeerCreationListener {
        void onPeerCreated(RTCSession session);
    }

    private VoIPRTCClient() {
        mWorkingExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public static VoIPRTCClient getInstance() {
        if (instance == null) {
            synchronized (VoIPRTCClient.class) {
                if (instance == null) {
                    instance = new VoIPRTCClient();
                }
            }
        }
        return instance;
    }

    public Executor getExecutor () {
        return mWorkingExecutor;
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
        mWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                createPeerFactoryInternal(context, factoryCreationListener);
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
    public void createPeerConnection (@NonNull final PeerCreationListener peerCreationListener,
                                      @Nullable final EglBase.Context renderEGLContext,
                                      @Nullable final VideoCapturer videoCapturer,
                                      @Nullable final VideoRenderer.Callbacks videoCallbackLocal,
                                      @Nullable final List<VideoRenderer.Callbacks> videoCallbacksRemote) {
        mWorkingExecutor.execute(new Runnable() {
            @Override
            public void run() {
                createPeerConnectionInternal(peerCreationListener, renderEGLContext,
                        videoCapturer, videoCallbackLocal, videoCallbacksRemote);
            }
        });
    }

    private void createPeerFactoryInternal(Context context,
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

        WebRtcAudioRecord.setErrorCallback(VoIPRTCClient.this);
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
            Logging.enableTracing(NATIVE_TRACE_USE, EnumSet.of
                    (Logging.TraceLevel.TRACE_DEFAULT));
            Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);
        }
        if (factoryCreationListener != null) {
            factoryCreationListener.onCreationDone(isSuccessful);
        }
    }

    /**
     * For creation PeerConnection with Video enable, we should also use
     * EglBase.Context renderEGLContext for PeerConnectionFactory
     */
    private void createPeerConnectionInternal(@NonNull PeerCreationListener peerCreationListener,
                                              @Nullable EglBase.Context renderEGLContext,
                                      @Nullable VideoCapturer videoCapturer,
                                      @Nullable VideoRenderer.Callbacks videoCallbackLocal,
                                      @Nullable List<VideoRenderer.Callbacks> videoCallbacksRemote) {
        if (mPeerFactory == null) {
            Log.e(TAG, "PeerConnection factory is not created");
            return;
        }
        RTCSession rtcSession = new RTCSession();
        PeerConnection peerConnection;
        DataChannel dataChannel = null;
        VideoTrack videoTrack = null;
        AudioTrack audioTrack;

        if (mPeerConnectionParameters.videoCallEnabled && renderEGLContext != null) {
            mPeerFactory.setVideoHwAccelerationOptions(renderEGLContext, renderEGLContext);
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
            dataChannel = peerConnection.createDataChannel(DATA_CHANNEL_UID, init);
            Log.d(TAG, "Created PeerConnection - DataChannel!");
        }
        /*
         * Checking Media Sources (Audio and Video), and
         * create/add them to the main MediaStream
         */
        checkAndCreateStream(videoCapturer);
        if (mPeerConnectionParameters.videoCallEnabled && videoCapturer != null
                && videoCallbackLocal != null) {
            videoTrack = createVideoTrack(videoCapturer, videoCallbackLocal);
            findVideoSender(peerConnection);
            mMediaStream.addTrack(videoTrack);
        }
        audioTrack = createAudioTrack();
        mMediaStream.addTrack(audioTrack);

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
        peerConnection.addStream(mMediaStream);
        peerCreationListener.onPeerCreated(rtcSession.configure
                (peerConnection, dataChannel, audioTrack, videoTrack));
    }



    private void checkAndCreateStream (@Nullable VideoCapturer videoCapturer) {
        if (mMediaStream == null) {
            mMediaStream = mPeerFactory.createLocalMediaStream(STREAM_UID);
        }
        if (mAudioSource == null) {
            mAudioSource = mPeerFactory.createAudioSource(mAudioConstraints);
        }
        if (mPeerConnectionParameters.videoCallEnabled
                && mVideoSource == null && videoCapturer != null) {
            mVideoSource = mPeerFactory.createVideoSource(videoCapturer);
        }
    }

    private AudioTrack createAudioTrack() {
        AudioTrack audioTrack = mPeerFactory.createAudioTrack(AUDIO_TRACK_ID, mAudioSource);
        audioTrack.setEnabled(true);
        return audioTrack;
    }

    /**
     * Only for video configuration!
     */
    private VideoTrack createVideoTrack(@NonNull VideoCapturer capturer,
                                        @NonNull VideoRenderer.Callbacks callbacks) {
        capturer.startCapture(mPeerConnectionParameters.videoWidth,
                mPeerConnectionParameters.videoHeight, mPeerConnectionParameters.videoFps);

        VideoTrack videoTrack = mPeerFactory.createVideoTrack(VIDEO_TRACK_ID, mVideoSource);
        videoTrack.setEnabled(true);
        videoTrack.addRenderer(new VideoRenderer(callbacks));
        return videoTrack;
    }

    /**
     * Only for video configuration!
     */
    @Nullable
    private RtpSender findVideoSender(@NonNull PeerConnection peerConnection) {
        for (RtpSender sender : peerConnection.getSenders()) {
            if (sender.track() != null) {
                String trackType = sender.track().kind();
                if (trackType.equals(VIDEO_TRACK_TYPE)) {
                    Log.d(TAG, "Found video sender.");
                    return sender;
                }
            }
        }
        return null;
    }



    private void createMediaConstraints() {
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
