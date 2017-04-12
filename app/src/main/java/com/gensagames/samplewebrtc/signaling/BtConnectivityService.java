package com.gensagames.samplewebrtc.signaling;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.gensagames.samplewebrtc.signaling.helper.ConnectivityChangeListener;
import com.gensagames.samplewebrtc.signaling.helper.MessageObservable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Matcher;

import static com.gensagames.samplewebrtc.signaling.BTConnectivityService.ConnectionState.IDLE;
import static com.gensagames.samplewebrtc.signaling.BTConnectivityService.ConnectionState.STATE_CONNECTED;
import static com.gensagames.samplewebrtc.signaling.BTConnectivityService.ConnectionState.STATE_CONNECTING;
import static com.gensagames.samplewebrtc.signaling.BTConnectivityService.ConnectionState.STATE_DISCONNECTED;
import static com.gensagames.samplewebrtc.signaling.BTConnectivityService.ConnectionState.STATE_LISTEN;

/**
 * Created by GensaGames
 * GensaGames
 */

public class BTConnectivityService {

    private static final String TAG = BTConnectivityService.class.getSimpleName();
    private static final String SERVICE_NAME = BTConnectivityService.class.getSimpleName();

    private static final String START_MSG_TAG = "<START>";
    private static final String END_MSG_TAG = "<END>";

    private final BluetoothAdapter mAdapter;
    private final MessageObservable mMessageObservable;
    private final UUID mMainAppUuid = UUID.nameUUIDFromBytes(SERVICE_NAME.getBytes());
    private ConnectivityChangeListener mConnectivityChangeListener;

    private CalleeTask mCalleeTask;
    private CallerTask mCallerTask;
    private BluetoothDevice mWorkingDevice;
    private MainSignalingTask mMainSignalingTask;
    private ConnectionState mState;


    public enum ConnectionState {
        IDLE,
        STATE_LISTEN,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_DISCONNECTED;

        public boolean isWorking() {
            return this == STATE_CONNECTED
                    || this == STATE_CONNECTING;
        }
    }

    public BTConnectivityService(MessageObservable messageObservable) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mMessageObservable = messageObservable;
        mState = ConnectionState.IDLE;
    }

    private synchronized void setState(ConnectionState state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        if (mConnectivityChangeListener != null) {
            mConnectivityChangeListener.onConnectivityStateChanged(state);
        }
    }

    public BluetoothDevice getWorkingDevice() {
        return mWorkingDevice;
    }

    public void setWorkingDevice(BluetoothDevice mWorkingDevice) {
        this.mWorkingDevice = mWorkingDevice;
    }

    public synchronized ConnectionState getState() {
        return mState;
    }

    public void setConnectivityChangeListener (ConnectivityChangeListener listener) {
        mConnectivityChangeListener = listener;
    }

    /**
     * Start the chat service. Specifically start CalleeTask to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (mAdapter == null || !mAdapter.isEnabled()) {
            Log.e(TAG, "Cannot start service!");
            return;
        }
        Log.d(TAG, "start");
        if (mCallerTask != null) {
            mCallerTask.cancel();
            mCallerTask = null;
        }
        if (mMainSignalingTask != null) {
            mMainSignalingTask.cancel();
            mMainSignalingTask = null;
        }
        if (mCalleeTask == null) {
            mCalleeTask = new CalleeTask();
            mCalleeTask.start();
        }
        setState(ConnectionState.STATE_LISTEN);
    }
    /**
     * Start the CallerTask to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        if (mState == STATE_CONNECTING) {
            if (mCallerTask != null) {
                mCallerTask.cancel();
                mCallerTask = null;
            }
        }
        if (mMainSignalingTask != null) {
            mMainSignalingTask.cancel();
            mMainSignalingTask = null;
        }
        // Start the thread to connect with the given device
        mWorkingDevice = device;
        mCallerTask = new CallerTask(device);
        mCallerTask.start();
        setState(STATE_CONNECTING);
    }
    /**
     * Start the MainSignalingTask to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        if (mCallerTask != null) {
            mCallerTask.cancel();
            mCallerTask = null;
        }

        if (mMainSignalingTask != null) {
            mMainSignalingTask.cancel();
            mMainSignalingTask = null;
        }

        if (mCalleeTask != null) {
            mCalleeTask.cancel();
            mCalleeTask = null;
        }
        /* Start the thread to manage the connection
         * and perform transmissions
         */
        mMainSignalingTask = new MainSignalingTask(socket);
        mMainSignalingTask.start();
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log .d(TAG, "stop");
        if (mCallerTask != null) {
            mCallerTask.cancel();
            mCallerTask = null;
        }
        if (mMainSignalingTask != null) {
            mMainSignalingTask.cancel();
            mMainSignalingTask = null;
        }
        if (mCalleeTask != null) {
            mCalleeTask.cancel();
            mCalleeTask = null;
        }
        setState(IDLE);
    }

    /**
     * Write to the MainSignalingTask in an unsynchronized manner
     */
    public void write(String msg) {
        MainSignalingTask r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mMainSignalingTask;
        }
        String msgToSend = START_MSG_TAG + msg + END_MSG_TAG;
        r.write(msgToSend.getBytes());
    }

    private void connectionFailed() {
        mWorkingDevice = null;
        setState(STATE_DISCONNECTED);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class CalleeTask extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        private CalleeTask() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, mMainAppUuid);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            Log.d(TAG, "BEGIN mCalleeTask" + this);
            setName(CalleeTask.class.getSimpleName());
            BluetoothSocket socket;

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    synchronized (BTConnectivityService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                mWorkingDevice = socket.getRemoteDevice();
                                connected(socket, mWorkingDevice);
                                break;
                            case IDLE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mCalleeTask");
        }
        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class CallerTask extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        CallerTask(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(mMainAppUuid);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i(TAG, "BEGIN mCallerTask");
            setName(CallerTask.class.getSimpleName());
            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.i(TAG, "Trying to connect failed! ", e);
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket" +
                            " during connection failure", e2);
                }
                BTConnectivityService.this.start();
                connectionFailed();
                return;
            }
            synchronized (BTConnectivityService.this) {
                mCallerTask = null;
            }
            connected(mmSocket, mmDevice);
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class MainSignalingTask extends Thread {
        private String mMainMsgBuffer;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public MainSignalingTask(BluetoothSocket socket) {
            Log.d(TAG, "create MainSignalingTask");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            Log.i(TAG, "BEGIN mMainSignalingTask");
            setName(MainSignalingTask.class.getSimpleName());
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    mMainMsgBuffer+= new String(buffer, 0, bytes);
                    mMainMsgBuffer = searchLocalMsg(mMainMsgBuffer);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionFailed();
                    break;
                }
            }
        }

        private String searchLocalMsg(String sourceMsg) {
            Log.d(TAG, "searchLocalMsg from: " + sourceMsg);
            try {
                while (sourceMsg.contains(START_MSG_TAG)
                        && sourceMsg.contains(END_MSG_TAG)) {
                    int startIndex = sourceMsg.indexOf(START_MSG_TAG)
                            + START_MSG_TAG.length();
                    int endIndex = sourceMsg.indexOf(END_MSG_TAG);
                    int endIndexToTrim = endIndex + END_MSG_TAG.length();

                    String fireMsg = sourceMsg.substring(startIndex, endIndex);
                    mMessageObservable.onReceiveMsg(fireMsg);
                    sourceMsg = sourceMsg.substring(endIndexToTrim);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error resolving incoming msg!", ex);
            }
            return sourceMsg;
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mMessageObservable.onSentMsg(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    /*
    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
            .sendToTarget();

    mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
        .sendToTarget();
        */
}