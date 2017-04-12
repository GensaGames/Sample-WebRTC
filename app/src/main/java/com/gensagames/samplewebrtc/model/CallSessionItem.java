package com.gensagames.samplewebrtc.model;

import java.io.Serializable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class CallSessionItem implements Serializable {

    public enum CallState {
        IDLE,
        CALLING,
        RINGING,
        INCOMING,
        CONNECTED,
        DISCONNECTED,
        FAILED
    }

    private String remoteName;
    private String bluetoothAddress;
    private CallState connectionState;
    private long sessionId;

    public CallSessionItem(String remoteName, String bluetoothAddress) {
        this.remoteName = remoteName;
        this.bluetoothAddress = bluetoothAddress;
    }

    public CallSessionItem(String remoteName, long sessionId, CallState connectionState) {
        this.remoteName = remoteName;
        this.sessionId = sessionId;
        this.connectionState = connectionState;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public CallState getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(CallState connectionState) {
        this.connectionState = connectionState;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }
}
