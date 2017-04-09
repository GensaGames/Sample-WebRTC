package com.gensagames.samplewebrtc.view.helper;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by GensaGames
 * GensaGames
 */
public class CollapseAppBarLayoutBehavior extends AppBarLayout.Behavior {
    private boolean mEnabled;

    public CollapseAppBarLayoutBehavior() {
        super();
    }

    public CollapseAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        return mEnabled && super.onStartNestedScroll(parent, child, directTargetChild,
                target, nestedScrollAxes);
    }

    public boolean isEnabled() {
        return mEnabled;
    }
}
