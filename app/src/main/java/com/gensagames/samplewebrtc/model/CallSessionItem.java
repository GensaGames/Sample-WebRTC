package com.gensagames.samplewebrtc.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.engine.utils.ProxyRenderer;

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
    private String action = VoIPEngineService.ACTION_IDLE;

    private ProxyRenderer localProxyRenderer;
    private ProxyRenderer remoteProxyRenderer;

    private CallState connectionState = CallState.IDLE;
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

    @NonNull
    public CallState getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(@NonNull CallState connectionState) {
        this.connectionState = connectionState;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    @NonNull
    public String getAction() {
        return action;
    }

    public void setAction(@NonNull String action) {
        this.action = action;
    }

    @Nullable
    public ProxyRenderer getLocalProxyRenderer() {
        return localProxyRenderer;
    }

    public void setLocalProxyRenderer(ProxyRenderer localProxyRenderer) {
        this.localProxyRenderer = localProxyRenderer;
    }

    @Nullable
    public ProxyRenderer getRemoteProxyRenderer() {
        return remoteProxyRenderer;
    }

    public void setRemoteProxyRenderer(ProxyRenderer remoteProxyRenderer) {
        this.remoteProxyRenderer = remoteProxyRenderer;
    }
}
