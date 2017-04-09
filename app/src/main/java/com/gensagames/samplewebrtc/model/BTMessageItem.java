package com.gensagames.samplewebrtc.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.webrtc.SessionDescription;

import java.io.Serializable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BTMessageItem implements Serializable{
    private String userName;
    private MessageType msgType;
    private String additionalContent;

    /**
     * Specific only for WebRTC usage
     */
    private long peerSessionId;
    private SessionDescription workingSdp;


    public enum MessageType {
        NONE,
        SDP_EXCHANGE
    }

    public BTMessageItem(@NonNull String userName, long peerSessionId,
                         @NonNull MessageType msgType, @Nullable SessionDescription workingSdp,
                         @Nullable String additionalContent) {
        this.userName = userName;
        this.peerSessionId = peerSessionId;
        this.msgType = msgType;
        this.workingSdp = workingSdp;
        this.additionalContent = additionalContent;
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
