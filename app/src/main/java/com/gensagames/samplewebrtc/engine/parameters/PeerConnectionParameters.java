package com.gensagames.samplewebrtc.engine.parameters;

/**
 * Created by GensaGames
 * GensaGames
 */

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Peer connection parameters.
 */
@SuppressWarnings("WeakerAccess")
public class PeerConnectionParameters {

    public final List<PeerConnection.IceServer> iceServers = new LinkedList<>();
    public final boolean videoCallEnabled;
    public final boolean loopback;
    public final boolean tracing;
    public final int videoWidth;
    public final int videoHeight;
    public final int videoFps;
    public final int videoMaxBitrate;
    public final String videoCodec;
    public final boolean videoCodecHwAcceleration;
    public final boolean videoFlexfecEnabled;
    public final int audioStartBitrate;
    public final String audioCodec;
    public final boolean noAudioProcessing;
    public final boolean aecDump;
    public final boolean useOpenSLES;
    public final boolean disableBuiltInAEC;
    public final boolean disableBuiltInAGC;
    public final boolean disableBuiltInNS;
    public final boolean enableLevelControl;
    public final DataChannelParameters dataChannelParameters;

    public PeerConnectionParameters(boolean videoCallEnabled, boolean loopback, boolean tracing,
                                    int videoWidth, int videoHeight, int videoFps, int videoMaxBitrate, String videoCodec,
                                    boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled, int audioStartBitrate,
                                    String audioCodec, boolean noAudioProcessing, boolean aecDump, boolean useOpenSLES,
                                    boolean disableBuiltInAEC, boolean disableBuiltInAGC, boolean disableBuiltInNS,
                                    boolean enableLevelControl) {
        this(videoCallEnabled, null, loopback, tracing, videoWidth, videoHeight, videoFps, videoMaxBitrate,
                videoCodec, videoCodecHwAcceleration, videoFlexfecEnabled, audioStartBitrate, audioCodec,
                noAudioProcessing, aecDump, useOpenSLES, disableBuiltInAEC, disableBuiltInAGC,
                disableBuiltInNS, enableLevelControl, null);
    }

    public PeerConnectionParameters(boolean videoCallEnabled, List<PeerConnection.IceServer> iceServers, boolean loopback, boolean tracing,
                                    int videoWidth, int videoHeight, int videoFps, int videoMaxBitrate, String videoCodec,
                                    boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled, int audioStartBitrate,
                                    String audioCodec, boolean noAudioProcessing, boolean aecDump, boolean useOpenSLES,
                                    boolean disableBuiltInAEC, boolean disableBuiltInAGC, boolean disableBuiltInNS,
                                    boolean enableLevelControl, DataChannelParameters dataChannelParameters) {
        if (iceServers != null) {
            this.iceServers.addAll(iceServers);
        }
        this.videoCallEnabled = videoCallEnabled;
        this.loopback = loopback;
        this.tracing = tracing;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoFps = videoFps;
        this.videoMaxBitrate = videoMaxBitrate;
        this.videoCodec = videoCodec;
        this.videoFlexfecEnabled = videoFlexfecEnabled;
        this.videoCodecHwAcceleration = videoCodecHwAcceleration;
        this.audioStartBitrate = audioStartBitrate;
        this.audioCodec = audioCodec;
        this.noAudioProcessing = noAudioProcessing;
        this.aecDump = aecDump;
        this.useOpenSLES = useOpenSLES;
        this.disableBuiltInAEC = disableBuiltInAEC;
        this.disableBuiltInAGC = disableBuiltInAGC;
        this.disableBuiltInNS = disableBuiltInNS;
        this.enableLevelControl = enableLevelControl;
        this.dataChannelParameters = dataChannelParameters;
    }

    public static PeerConnectionParameters getDefaultAudioOnly () {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer(Configs.GOOGLE_STUN_URI));
        return new PeerConnectionParameters(false, iceServers, false, true, Configs.HD_VIDEO_WIDTH,
                Configs.HD_VIDEO_HEIGHT, Configs.DEFAULT_FPS, Configs.DEFAULT_VIDEO_BITRATE,
                Configs.CODEC_DEFAULT, true, true, Configs.AUDIO_START_BITRATE, Configs.CODEC_DEFAULT,
                false, true, false, false, false, false, false, DataChannelParameters.getDefault());
    }
}