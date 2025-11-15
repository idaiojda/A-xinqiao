package com.example.xinqiao.view;

import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.runtime.Composer;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import com.example.xinqiao.community.CommunityScreenNewKt;
import com.example.xinqiao.community.CommunityController;

/**
 * “社区”主视图：嵌入 Compose CommunityScreen
 */
public class CommunityView extends FrameLayout {
    private View mView;
    private CommunityController controller;

    public CommunityView(AppCompatActivity activity) {
        super(activity);
        init();
    }

    private void init() {
        controller = new CommunityController();
        ComposeView composeView = new ComposeView(getContext());
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed.INSTANCE);
        composeView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        composeView.setContent(new Function2<Composer, Integer, Unit>() {
            @Override
            public Unit invoke(Composer composer, Integer changed) {
                CommunityScreenNewKt.CommunityScreenNew(controller, composer, 0);
                return Unit.INSTANCE;
            }
        });
        mView = composeView;
        addView(mView);
    }

    public View getView() {
        return this;
    }

    public void showView() {
        setVisibility(View.VISIBLE);
        if (mView != null) {
            mView.setVisibility(View.VISIBLE);
        }
    }

    public void hideView() {
        setVisibility(View.GONE);
    }

    /**
     * 供 Activity 返回键调用，若处于详情页则先返回到列表。
     * @return 是否已消费返回事件
     */
    public boolean handleBackPressed() {
        if (controller == null) return false;
        return controller.handleBackPressed();
    }
}
