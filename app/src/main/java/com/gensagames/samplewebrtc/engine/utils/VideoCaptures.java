package com.gensagames.samplewebrtc.engine.utils;

import android.content.Context;
import android.util.Log;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.Logging;
import org.webrtc.VideoCapturer;

/**
 * Created by GensaGames
 * GensaGames
 */

/**
 * Helper class, taken from WebRTC Sample.
 * NOTE! We should support writeToTexture in any case!
 */
@SuppressWarnings("WeakerAccess")
public class VideoCaptures {
    private static final String TAG = VideoCaptures.class.getSimpleName();


    public static VideoCapturer createCorrectCapturer (Context appContext) {
        if (Camera2Enumerator.isSupported(appContext)) {
            Log.d(TAG, "Looking for front facing cameras.");
            return createCameraCapturer(new Camera2Enumerator(appContext));
        } else {
            Log.d(TAG, "Looking for front facing cameras.");
            return createCameraCapturer(new Camera1Enumerator(true));
        }
    }

    private static  VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }
}
