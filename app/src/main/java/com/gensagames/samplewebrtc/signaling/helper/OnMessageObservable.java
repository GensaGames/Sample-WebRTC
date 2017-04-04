package com.gensagames.samplewebrtc.signaling.helper;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface OnMessageObservable {
    void onReceiveMsg (byte[] msgBytes, int length);
    void onSentMsg (byte[] msgBytes);
}
