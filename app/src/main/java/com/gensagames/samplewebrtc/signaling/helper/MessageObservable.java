package com.gensagames.samplewebrtc.signaling.helper;

/**
 * Created by GensaGames
 * GensaGames
 */

public interface MessageObservable {
    void onReceiveMsg (String msg);
    void onSentMsg (byte[] msgBytes);
}
