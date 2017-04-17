package com.gensagames.samplewebrtc.engine.utils;

import android.util.Log;

import org.webrtc.VideoRenderer;

import java.io.Serializable;

/**
 * Created by GensaGames
 * GensaGames
 */

public class ProxyRenderer  implements VideoRenderer.Callbacks, Serializable {
    private static final String TAG = ProxyRenderer.class.getSimpleName();

    private transient VideoRenderer.Callbacks target;

    public synchronized void renderFrame(VideoRenderer.I420Frame frame) {
        if (target == null) {
            Log.d(TAG, "Dropping frame in proxy because target is null.");
            VideoRenderer.renderFrameDone(frame);
            return;
        }

        target.renderFrame(frame);
    }

    public synchronized void setTarget(VideoRenderer.Callbacks target) {
        this.target = target;
    }
}
