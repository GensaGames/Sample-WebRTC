package com.gensagames.samplewebrtc.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.Serializable;
import java.util.List;

/**
 * Created by GensaGames
 * GensaGames
 */

public class SignalingMessageItem implements Serializable {
    private String userName;
    private MessageType msgType;
    private String additionalContent;

    /**
     * Specific only for WebRTC usage
     */
    private long peerSessionId;
    private SessionDescription workingSdp;
    private List<IceCandidate> candidates;


    public enum MessageType {
        NONE,
        SDP_EXCHANGE,
        CANDIDATES;
    }

    public SignalingMessageItem(@NonNull String userName, long peerSessionId,
                                @NonNull MessageType msgType, @Nullable SessionDescription workingSdp,
                                @Nullable String additionalContent) {
        this.userName = userName;
        this.peerSessionId = peerSessionId;
        this.msgType = msgType;
        this.workingSdp = workingSdp;
        this.additionalContent = additionalContent;
    }

    public SignalingMessageItem(@NonNull String userName, long peerSessionId,
                                @NonNull MessageType msgType, @Nullable List<IceCandidate> candidates,
                                @Nullable SessionDescription workingSdp, @Nullable String additionalContent) {
        this.userName = userName;
        this.peerSessionId = peerSessionId;
        this.msgType = msgType;
        this.workingSdp = workingSdp;
        this.candidates = candidates;
        this.additionalContent = additionalContent;
    }

    public List<IceCandidate> getCandidates() {
        return candidates;
    }

    public String getUserName() {
        return userName;
    }

    public long getPeerSessionId() {
        return peerSessionId;
    }

    public MessageType getMessageType() {
        return msgType;
    }

    public SessionDescription getWorkingSdp() {
        return workingSdp;
    }

    public String getAdditionalContent() {
        return additionalContent;
    }
}
