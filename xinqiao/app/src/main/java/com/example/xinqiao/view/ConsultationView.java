package com.example.xinqiao.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import com.example.xinqiao.R;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.runtime.Composer;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import com.example.xinqiao.consultation.pro.ConsultProScreenKt;
import com.example.xinqiao.consultation.AiFloatingStateViewModel;
import com.example.xinqiao.consultation.ChatViewModel;
import com.example.xinqiao.consultation.FloatingAiWindowManager;

/**
 * “咨询”主视图：默认展示专业咨询占位，并挂载 AI 悬浮窗。
 */
public class ConsultationView extends FrameLayout {
    private final AppCompatActivity activity;
    private View mView;
    private FloatingAiWindowManager floatingManager;
    private AiFloatingStateViewModel stateVM;
    private ChatViewModel chatVM;

    public ConsultationView(AppCompatActivity activity) {
        super(activity);
        this.activity = activity;
        init();
    }

    private void init() {
        floatingManager = new FloatingAiWindowManager(activity);

        // SavedState 注入的 ViewModel（跨页面持久化）
        ViewModelProvider provider = new ViewModelProvider(activity,
                new SavedStateViewModelFactory(activity.getApplication(), activity));
        stateVM = provider.get(AiFloatingStateViewModel.class);
        chatVM = provider.get(ChatViewModel.class);

        // 嵌入 Compose 专业咨询页
        ComposeView composeView = new ComposeView(getContext());
        composeView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        composeView.setContent(new Function2<Composer, Integer, Unit>() {
            @Override
            public Unit invoke(Composer composer, Integer changed) {
                // 传入默认参数位掩码，令 vm 使用默认的 viewModel()
                ConsultProScreenKt.ConsultProScreen(null, composer, 0, 1);
                return Unit.INSTANCE;
            }
        });
        mView = composeView;
        addView(mView);
    }

    public View getView() {
        // 返回容器自身，确保能被主页面正确添加/隐藏
        return this;
    }

    public void showView() {
        // 显示容器和内容，并挂载悬浮窗
        setVisibility(View.VISIBLE);
        if (mView != null) {
            mView.setVisibility(View.VISIBLE);
        }
        floatingManager.attach(stateVM, chatVM);
    }

    public void hideView() {
        // 隐藏整个容器，底部悬浮窗保持当前状态（不主动移除）
        setVisibility(View.GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 页面被移除或 Activity 销毁时，确保移除悬浮窗，避免 WindowLeaked
        if (floatingManager != null) {
            floatingManager.detach();
        }
    }
}
