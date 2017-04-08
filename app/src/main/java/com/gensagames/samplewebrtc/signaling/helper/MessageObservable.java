package com.gensagames.samplewebrtc.signaling.helper;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface MessageObservable {
    void onReceiveMsg (byte[] msgBytes, int length);
    void onSentMsg (byte[] msgBytes);
}
