package com.example.xinqiao.view;

import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.runtime.Composer;
import androidx.compose.ui.platform.ComposeView;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import com.example.xinqiao.community.NotificationItem;
import com.example.xinqiao.community.CommunityRepositoryProvider;

public class NotificationsView extends FrameLayout {
    private final AppCompatActivity activity;
    private View mView;

    public NotificationsView(AppCompatActivity activity) {
        super(activity);
        this.activity = activity;
        init();
    }

    private void init() {
        ComposeView composeView = new ComposeView(getContext());
        composeView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        composeView.setContent(new Function2<Composer, Integer, Unit>() {
            @Override
            public Unit invoke(Composer composer, Integer changed) {
                NotificationsViewKt.NotificationsScreen(composer, 0);
                return Unit.INSTANCE;
            }
        });
        mView = composeView;
        addView(mView);
    }

    public View getView() { return this; }
    public void showView() { setVisibility(View.VISIBLE); if (mView != null) mView.setVisibility(View.VISIBLE); }
    public void hideView() { setVisibility(View.GONE); }
}

