package com.gensagames.samplewebrtc.view.helper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GensaGames
 * GensaGames
 */

public abstract class BaseActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private static final String TAG = BaseActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 101;
    private boolean mIsFirstStart;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsFirstStart = !mIsFirstStart;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus && mIsFirstStart) {
            mIsFirstStart = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }
        List<String> deniedPermission = null;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (deniedPermission == null) {
                    deniedPermission = new ArrayList<>();
                }
                deniedPermission.add(permissions[i]);
            }
        }
        if (deniedPermission != null && !deniedPermission.isEmpty()) {
            onPermissionsDenied(deniedPermission);
            return;
        }
        onPermissionsGranted();
    }

    public void checkActivityPermissions() {
        List<String> requiresListPermission = requiredPermissions();
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.LOLLIPOP ||
                requiresListPermission == null || requiresListPermission.isEmpty()) {
            onPermissionsGranted();
            return;
        }
        List<String> grantedListPermissions = new ArrayList<>();
        for (String permission : requiresListPermission) {
            int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                grantedListPermissions.add(permission);
            }
        }
        requiresListPermission.removeAll(grantedListPermissions);
        if (!requiresListPermission.isEmpty()) {
            makeRequest(requiresListPermission);
            return;
        }
        onPermissionsGranted();
    }


    protected void makeRequest(List<String> permissionsList) {
        ActivityCompat.requestPermissions(this, permissionsList
                .toArray(new String[permissionsList.size()]), PERMISSION_REQUEST_CODE);
    }

    public abstract ArrayList<String> requiredPermissions();

    public abstract void onPermissionsDenied(List<String> deniedPermissions);

    public abstract void onPermissionsGranted();
}