package com.gensagames.samplewebrtc.view.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
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
import android.widget.TextView;

import com.gensagames.samplewebrtc.R;
import com.gensagames.samplewebrtc.engine.RTCClient;
import com.gensagames.samplewebrtc.engine.VoIPEngineService;
import com.gensagames.samplewebrtc.engine.utils.ProxyRenderer;
import com.gensagames.samplewebrtc.model.BluetoothDeviceItem;
import com.gensagames.samplewebrtc.model.CallSessionItem;
import com.gensagames.samplewebrtc.view.helper.CollapseAppBarLayoutBehavior;
import com.gensagames.samplewebrtc.view.helper.FragmentHeaderTransaction;
import com.gensagames.samplewebrtc.view.helper.OnSliderPageSelected;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.io.Serializable;
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
    private IncomingCallReceiver mIncomingCallReceiver;

    private View mCollapsingParent;
    private TextView mCollapsingText1;
    private TextView mCollapsingText2;

    private View mVideoPanel;
    private SurfaceViewRenderer pipRenderer;
    private SurfaceViewRenderer fullscreenRenderer;
    private final List<VideoRenderer.Callbacks> remoteRenderers =
            new ArrayList<VideoRenderer.Callbacks>();
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
        getActivity().unregisterReceiver(mIncomingCallReceiver);
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
        mIncomingCallReceiver = new IncomingCallReceiver();
        IntentFilter intentFilter = new IntentFilter(VoIPEngineService.NOTIFY_INCOMING_CALL);
        intentFilter.addAction(VoIPEngineService.NOTIFY_CALL_CONNECTED);
        intentFilter.addAction(VoIPEngineService.NOTIFY_CALL_DISCONNECTED);
        intentFilter.addAction(VoIPEngineService.NOTIFY_OUTGOING_CALL);
        getActivity().registerReceiver(mIncomingCallReceiver, intentFilter);
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
        Activity activity = getActivity();
        Intent intent = new Intent(action, Uri.EMPTY, activity, VoIPEngineService.class);
        intent.putExtra(VoIPEngineService.EXTRA_CALL_SESSION, mLastSessionItem);

        if (action.equals(VoIPEngineService.ACTION_ANSWER_CALL)) {
            intent.putExtra(VoIPEngineService.EXTRA_LOCAL_RENDERER, localProxyRenderer);
            intent.putExtra(VoIPEngineService.EXTRA_REMOTE_RENDERER, remoteProxyRenderer);
        }
        activity.startService(intent);
    }

    public void notifyStartCall (BluetoothDeviceItem device) {
        Activity activityContext = getActivity();
        Intent intent = new Intent(VoIPEngineService.ACTION_START_CALL, Uri.EMPTY,
                activityContext, VoIPEngineService.class);
        intent.putExtra(VoIPEngineService.EXTRA_CALL_SESSION,
                new CallSessionItem(device.getDeviceName(), device.getDeviceAddress()));

        intent.putExtra(VoIPEngineService.EXTRA_LOCAL_RENDERER, localProxyRenderer);
        intent.putExtra(VoIPEngineService.EXTRA_REMOTE_RENDERER, remoteProxyRenderer);
        activityContext.startService(intent);
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
    private class IncomingCallReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            CallSessionItem session = (CallSessionItem) intent
                    .getSerializableExtra(VoIPEngineService.EXTRA_CALL_SESSION);

            switch (action) {
                case VoIPEngineService.NOTIFY_INCOMING_CALL:
                    handleIncomingCall(session);
                    break;
                case VoIPEngineService.NOTIFY_CALL_CONNECTED:
                    handleCallConnected(session);
                    break;
                case VoIPEngineService.NOTIFY_CALL_DISCONNECTED:
                    handleCallDisconnected(session);
                    break;
                case VoIPEngineService.NOTIFY_OUTGOING_CALL:
                    handleOutgoingCall(session);
                    break;
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
