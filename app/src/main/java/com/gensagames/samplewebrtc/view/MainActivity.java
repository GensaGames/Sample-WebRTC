package com.gensagames.samplewebrtc.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.model.BTMessageItem;
import com.gensagames.samplewebrtc.view.fragments.MainSliderFragment;
import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.view.fragments.AboutFragment;
import com.gensagames.samplewebrtc.view.helper.FragmentHeaderTransaction;

public class MainActivity extends AppCompatActivity {

    private View mHeaderInstanceView;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private FragmentManager mFragmentManager;
    private CoordinatorLayout mCoordinatorLayout;

    private IncomingCallReceiver mIncomingCallReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setNavigationDrawer();

        startService(new Intent(VoIPEngineService.ACTION_IDLE, Uri.EMPTY,
                getApplicationContext(), VoIPEngineService.class));
        registerVoIPReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mIncomingCallReceiver);
    }

    private void registerVoIPReceiver () {
        mIncomingCallReceiver = new IncomingCallReceiver();
        IntentFilter intentFilter = new IntentFilter(VoIPEngineService.ANNOUNCE_INCOMING_CALL);
        registerReceiver(mIncomingCallReceiver, intentFilter);
    }

    private void handleIncomingCall(BTMessageItem item) {

        /**
         * TODO(UI) After some actions
         */
        Intent intent = new Intent(VoIPEngineService.ACTION_ANSWER_CALL, Uri.EMPTY,
                this, VoIPEngineService.class);
        intent.putExtra(VoIPEngineService.EXTRA_BT_MSG, item);
        startService(intent);
    }

    private void setNavigationDrawer () {
        mHeaderInstanceView = findViewById(R.id.headerContainer);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.mainCoordinator);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.mainDrawerLayout);
        mNavigationView = (NavigationView) findViewById(R.id.mainDrawerNavigation);

        mFragmentManager = getSupportFragmentManager();
        makeFragmentTransaction(new MainSliderFragment());

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                mDrawerLayout.closeDrawers();
                Fragment fragment = null;

                if (menuItem.getItemId() == R.id.nav_item_actions) {
                    fragment = new MainSliderFragment();
                }
                if (menuItem.getItemId() == R.id.nav_item_about) {
                    fragment = new AboutFragment();
                }

                if (fragment != null) {
                    makeFragmentTransaction(fragment);
                }

                return false;
            }

        });
    }

    private void makeFragmentTransaction (Fragment fragment) {

        int insertIndex = mCoordinatorLayout.indexOfChild(mHeaderInstanceView);
        int nextHeaderResource = R.layout.header_default;
        /*
         * Include Header changes, where it's possible
         */
        if (fragment instanceof FragmentHeaderTransaction) {
            nextHeaderResource = ((FragmentHeaderTransaction) fragment)
                    .getHeaderLayouts();
        }

        mCoordinatorLayout.removeView(mHeaderInstanceView);
        mHeaderInstanceView = getLayoutInflater().inflate(nextHeaderResource, null);
        mCoordinatorLayout.addView(mHeaderInstanceView, insertIndex, new CoordinatorLayout.
                LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                        CoordinatorLayout.LayoutParams.WRAP_CONTENT));

        setDrawerToggle();
        mFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /**
     * Setup Drawer Toggle of the Toolbar
     */
    private void setDrawerToggle () {
        android.support.v7.widget.Toolbar toolbar =
                (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, toolbar, R.string.app_name,
                R.string.app_name);

        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    private  class IncomingCallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(VoIPEngineService.ANNOUNCE_INCOMING_CALL)) {
                handleIncomingCall((BTMessageItem) intent
                        .getSerializableExtra(VoIPEngineService.EXTRA_BT_MSG));
            }
        }

    }
}