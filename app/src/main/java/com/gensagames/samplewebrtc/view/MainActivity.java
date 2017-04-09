package com.gensagames.samplewebrtc.view;

import android.content.Intent;
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
import com.gensagames.samplewebrtc.view.fragments.MainSliderFragment;
import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.view.fragments.AboutFragment;
import com.gensagames.samplewebrtc.view.helper.CollapseAppBarLayoutBehavior;
import com.gensagames.samplewebrtc.view.helper.FragmentHeaderTransaction;

public class MainActivity extends AppCompatActivity {

    private View mHeaderInstanceView;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private FragmentManager mFragmentManager;
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setNavigationDrawer();

        startService(new Intent(VoIPEngineService.ACTION_IDLE, Uri.EMPTY,
                getApplicationContext(), VoIPEngineService.class));
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
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.
                LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT);
        params.setBehavior(new CollapseAppBarLayoutBehavior());
        mCoordinatorLayout.addView(mHeaderInstanceView, insertIndex, params);

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

}