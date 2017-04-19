package com.gensagames.samplewebrtc.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.engine.RTCClient;
import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.engine.utils.ProxyRenderer;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.model.CallSessionItem;
import com.gensagames.samplewebrtc.model.SignalingMessageItem;
import com.gensagames.samplewebrtc.view.helper.CollapseAppBarLayoutBehavior;
import com.gensagames.samplewebrtc.view.helper.FragmentHeaderTransaction;
import com.gensagames.samplewebrtc.view.helper.OnSliderPageSelected;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;
import java.util.List;

public class MainSliderFragment extends Fragment implements FragmentHeaderTransaction,
        ViewPager.OnPageChangeListener, AppBarLayout.OnOffsetChangedListener {

    private static final String TAG = MainSliderFragment.class.getSimpleName();

    private CallSessionItem mLastSessionItem;
    private View mBtnAnswerView;
    private View mBtnHangupView;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingLayout;
    private LocalFragmentAdapter mLocalFragmentAdapter;
    private VoIPEngineListener mVoIPEngineListener;

    private View mCollapsingParent;
    private TextView mCollapsingText1;
    private TextView mCollapsingText2;

    private View mVideoPanel;
    private SurfaceViewRenderer pipRenderer;
    private SurfaceViewRenderer fullscreenRenderer;
    private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer();
    private final ProxyRenderer localProxyRenderer = new ProxyRenderer();


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
        mCollapsingParent =  getActivity().findViewById(R.id.fragmentCollapsingTextParent);
        mCollapsingText1 = (TextView) getActivity().findViewById(R.id.collapsingText1);
        mCollapsingText2 = (TextView) getActivity().findViewById(R.id.collapsingText2);

        mViewPager = (ViewPager) x.findViewById(R.id.viewpager);
        mBtnAnswerView = x.findViewById(R.id.fragmentBtnAnswer);
        mBtnHangupView = x.findViewById(R.id.fragmentBtnHangup);
        mVideoPanel = x.findViewById(R.id.fragmentVideoPanel);
        pipRenderer = (SurfaceViewRenderer) x.findViewById(R.id.pip_video_view);
        fullscreenRenderer = (SurfaceViewRenderer) x.findViewById(R.id.fullscreen_video_view);

        setupViewPager();
        setupVideoRenderer();
        registerVoIPReceiver();
        return x;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        VoIPEngineService.getInstance()
                .removeEngineEventListener(mVoIPEngineListener);
    }


    private void setupVideoRenderer () {
        pipRenderer.init(RTCClient.getInstance().getRootEglBase().getEglBaseContext(), null);
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        fullscreenRenderer.init(RTCClient.getInstance().getRootEglBase().getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true);
        fullscreenRenderer.setEnableHardwareScaler(true);

        localProxyRenderer.setTarget(fullscreenRenderer);
        remoteProxyRenderer.setTarget(pipRenderer);
        fullscreenRenderer.setMirror(true);
        pipRenderer.setMirror(false);
    }


    private void handleCallDisconnected (CallSessionItem item) {
        enableCollapseToolbar(false);
        mBtnHangupView.setEnabled(false);
        mBtnAnswerView.setEnabled(false);
        mCollapsingText1.setText(getString(R.string.state_disconnected_call));
        mCollapsingText2.setText(getString(R.string.state_idle));

        mBtnAnswerView.setVisibility(View.GONE);
        mBtnHangupView.setVisibility(View.GONE);
        mVideoPanel.setVisibility(View.GONE);

        mLastSessionItem = item;
        notifyService(VoIPEngineService.ACTION_HANGUP_CALL);
    }

    private void handleOutgoingCall (final CallSessionItem item) {
        enableCollapseToolbar(true);
        mBtnHangupView.setEnabled(true);
        mBtnAnswerView.setEnabled(false);
        mCollapsingText1.setText(item.getRemoteName());
        mCollapsingText2.setText(getString(R.string.state_outgoing_call));

        mBtnHangupView.setVisibility(View.VISIBLE);
        mBtnAnswerView.setVisibility(View.GONE);


        mLastSessionItem = item;
        mBtnHangupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyService(VoIPEngineService.ACTION_HANGUP_CALL);
            }
        });
    }

    private void handleCallConnected (final CallSessionItem item) {
        enableCollapseToolbar(true);
        mBtnHangupView.setEnabled(true);
        mBtnAnswerView.setEnabled(false);
        mCollapsingText1.setText(item.getRemoteName());
        mCollapsingText2.setText(getString(R.string.state_connected_call));
        mBtnHangupView.setVisibility(View.VISIBLE);
        mBtnAnswerView.setVisibility(View.GONE);
        mVideoPanel.setVisibility(View.VISIBLE);

        mLastSessionItem = item;
        mBtnHangupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyService(VoIPEngineService.ACTION_HANGUP_CALL);
            }
        });
    }

    private void handleIncomingCall(final CallSessionItem item) {
        enableCollapseToolbar(true);
        mBtnHangupView.setEnabled(false);
        mBtnAnswerView.setEnabled(true);
        mCollapsingText1.setText(item.getRemoteName());
        mCollapsingText2.setText(getString(R.string.state_incoming_call));
        mBtnAnswerView.setVisibility(View.VISIBLE);

        mLastSessionItem = item;
        mBtnAnswerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyService(VoIPEngineService.ACTION_ANSWER_CALL);
            }
        });
    }

    private void registerVoIPReceiver () {
        mVoIPEngineListener = new VoIPEngineListener();
        VoIPEngineService.getInstance()
                .addEngineEventListener(mVoIPEngineListener);
    }


    private void enableCollapseToolbar (boolean enabled) {
        mAppBarLayout.setExpanded(enabled, true);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams)
                mAppBarLayout.getLayoutParams();

        CollapseAppBarLayoutBehavior behavior = (CollapseAppBarLayoutBehavior)
                layoutParams.getBehavior();
        if (behavior != null) {
            behavior.setEnabled(enabled);
        }
    }


    private void setupViewPager () {
        enableCollapseToolbar(false);
        mCollapsingLayout.setTitle("");
        mCollapsingText1.setText(getString(R.string.app_name));
        mCollapsingText2.setText(getString(R.string.state_idle));

        mAppBarLayout.addOnOffsetChangedListener(this);
        mLocalFragmentAdapter = new LocalFragmentAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mLocalFragmentAdapter);

        mViewPager.addOnPageChangeListener(this);
        mTabLayout.setupWithViewPager(mViewPager);
    }


    private void notifyService (String action) {
        mLastSessionItem.setAction(action);

        if (action.equals(VoIPEngineService.ACTION_ANSWER_CALL)) {
            mLastSessionItem.setLocalProxyRenderer(localProxyRenderer);
            mLastSessionItem.setRemoteProxyRenderer(remoteProxyRenderer);
        }
        VoIPEngineService.getInstance().onStartCommand(mLastSessionItem);
    }

    public void notifyStartCall (BluetoothDeviceItem device) {
        CallSessionItem item = new CallSessionItem(device.getDeviceName(),
                device.getDeviceAddress());

        item.setAction(VoIPEngineService.ACTION_START_CALL);
        item.setLocalProxyRenderer(localProxyRenderer);
        item.setRemoteProxyRenderer(remoteProxyRenderer);
        VoIPEngineService.getInstance().onStartCommand(item);
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

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        int totalScroll = appBarLayout.getTotalScrollRange();
        float currentScrollPercentage = (float) Math.abs(verticalOffset)
                / totalScroll;
        float updatedScale = 1.2f +
                ((1 - currentScrollPercentage) * 1.0f);
        mCollapsingParent.setTranslationY((totalScroll - Math.abs(verticalOffset) +
                (1 - currentScrollPercentage) * 100 * -1));
        mCollapsingParent.setTranslationX((totalScroll - Math.abs(verticalOffset)) / 4);

        mCollapsingParent.setScaleX(updatedScale);
        mCollapsingParent.setScaleY(updatedScale);
    }


    /**
     * ****************************************************
     * Defined receiver for receiving call actions.
     */
    private class VoIPEngineListener implements VoIPEngineService.VoIPEngineEvents {

        private static final int CALL_SESSION_FRAGMENT_POS = 0;
        private Handler mLocalUiHandler = new Handler(Looper.getMainLooper());

        @Override
        public void onOutgoingCall(final CallSessionItem item) {
            mLocalUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleOutgoingCall(item);
                }
            });
        }

        @Override
        public void onIncomingCall(final CallSessionItem item) {
            mLocalUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleIncomingCall(item);
                }
            });
        }

        @Override
        public void onSignalingMsg(final SignalingMessageItem item) {
            mLocalUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Fragment fragment = mLocalFragmentAdapter.getItem(CALL_SESSION_FRAGMENT_POS);
                    if (fragment instanceof CallSessionFragment && fragment.isAdded()) {
                        ((CallSessionFragment) fragment).handleSignalingMsg(item);
                    }
                }
            });
        }

        @Override
        public void onConnected(final CallSessionItem item) {
            mLocalUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleCallConnected(item);
                }
            });
        }

        @Override
        public void onDisconnected(final CallSessionItem item) {
            mLocalUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleCallDisconnected(item);
                }
            });
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
