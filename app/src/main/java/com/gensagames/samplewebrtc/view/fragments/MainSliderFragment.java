package com.gensagames.samplewebrtc.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.model.BTMessageItem;
import com.gensagames.samplewebrtc.view.helper.DisableableAppBarLayoutBehavior;
import com.gensagames.samplewebrtc.view.helper.FragmentHeaderTransaction;
import com.gensagames.samplewebrtc.view.helper.OnSliderPageSelected;

public class MainSliderFragment extends Fragment implements FragmentHeaderTransaction,
        ViewPager.OnPageChangeListener {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private AppBarLayout mAppBarLayout;
    private LocalFragmentAdapter mLocalFragmentAdapter;
    private IncomingCallReceiver mIncomingCallReceiver;
    private CollapsingToolbarLayout mCollapsingLayout;

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View x = inflater.inflate(R.layout.fragment_slider, null);
        mAppBarLayout = (AppBarLayout) getActivity().
                findViewById(R.id.headerAppBarLayout);
        mCollapsingLayout = (CollapsingToolbarLayout) getActivity().
                findViewById(R.id.headerCollapsingToolbar);
        mTabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        mViewPager = (ViewPager) x.findViewById(R.id.viewpager);

        setupViewPager();
        registerVoIPReceiver();
        return x;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mIncomingCallReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        setExpandEnabled(false);
    }

    private void showIncomingCall(BTMessageItem item) {

    }

    private void answerIncomingCall (BTMessageItem item) {
        Activity activity = getActivity();
        Intent intent = new Intent(VoIPEngineService.ACTION_ANSWER_CALL, Uri.EMPTY,
                activity, VoIPEngineService.class);
        intent.putExtra(VoIPEngineService.EXTRA_BT_MSG, item);
        activity.startService(intent);
    }

    private void registerVoIPReceiver () {
        mIncomingCallReceiver = new IncomingCallReceiver();
        IntentFilter intentFilter = new IntentFilter(VoIPEngineService.ANNOUNCE_INCOMING_CALL);
        getActivity().registerReceiver(mIncomingCallReceiver, intentFilter);
    }



    private void setExpandEnabled(boolean enabled) {
    }


    private void setupViewPager () {
        mLocalFragmentAdapter = new LocalFragmentAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mLocalFragmentAdapter);

        mViewPager.addOnPageChangeListener(this);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public int getHeaderLayouts() {
        return R.layout.header_slider_fragment;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == 0) {
            Fragment fragment = mLocalFragmentAdapter.getItem(mViewPager.getCurrentItem());
            if (fragment instanceof OnSliderPageSelected) {
                ((OnSliderPageSelected) fragment).onThisPageSelected();
            }
        }
    }

    /**
     * ****************************************************
     * Defined receiver for receiving call actions.
     */
    private  class IncomingCallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(VoIPEngineService.ANNOUNCE_INCOMING_CALL)) {
                showIncomingCall((BTMessageItem) intent
                        .getSerializableExtra(VoIPEngineService.EXTRA_BT_MSG));
            }
        }

    }

    /**
     * ****************************************************
     * Main Adapter for Slider Fragment in this scope.
     */

    private class LocalFragmentAdapter extends FragmentPagerAdapter {
        final String[] SLIDER_ITEM_HEADERS = { MainSliderFragment.this
                .getString(R.string.slider_call_header), MainSliderFragment.this
                .getString(R.string.slider_device_header)};

        private Fragment callSessionFragment = new CallSessionFragment();
        private Fragment callListFragment = new CallListFragment();
        private LocalFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return callSessionFragment;
                case 1: return callListFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return SLIDER_ITEM_HEADERS.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return SLIDER_ITEM_HEADERS[position];
        }
    }

}
