package com.gensagames.samplewebrtc.view;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.widget.Toast;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.view.helper.BaseActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkActivityPermissions();
            }
        }, 500);
    }

    @Override
    public ArrayList<String> requiredPermissions() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.WRITE_SETTINGS, Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WAKE_LOCK, Manifest.permission.VIBRATE,
                Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.READ_LOGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            list.add(Manifest.permission.BIND_VOICE_INTERACTION);
        }
        return list;
    }

    @Override
    public void onPermissionsDenied(List<String> deniedPermissions) {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));

        for (String permission : deniedPermissions) {
            Toast.makeText(SplashActivity.this, "Permission denied: " + permission.split("\\.")[2],
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPermissionsGranted() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
    }
}
