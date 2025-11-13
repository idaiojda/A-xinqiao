package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.animation.ObjectAnimator;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.xinqiao.R;
import com.example.xinqiao.fragment.*;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MedicalRecordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_record_ultra_modern);

        // Ultra-modern title bar setup with advanced animations
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvSubtitle = findViewById(R.id.tv_subtitle);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        
        tvTitle.setText("我的诊疗档案");
        tvSubtitle.setText("健康管理 · 专业贴心");

        // Add advanced entrance animations
        animateTitleBarUltraModern(tvTitle, tvSubtitle, btnBack, btnSettings);
        
        // Add activity entrance animation
        overridePendingTransition(R.anim.slide_in_right_enhanced, R.anim.slide_out_left_enhanced);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left_enhanced);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add settings functionality here
                android.widget.Toast.makeText(MedicalRecordActivity.this, "设置功能开发中...", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Tabs + ViewPager2
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        MedicalRecordPagerAdapter adapter = new MedicalRecordPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("概览"); break;
                case 1: tab.setText("咨询记录"); break;
                case 2: tab.setText("测评报告"); break;
                case 3: tab.setText("情绪日记"); break;
                case 4: tab.setText("健康指标"); break;
            }
        }).attach();

        // 默认打开指定Tab
        int defaultTab = getIntent() != null ? getIntent().getIntExtra("default_tab", 0) : 0;
        if (defaultTab >= 0 && defaultTab < adapter.getItemCount()) {
            viewPager.setCurrentItem(defaultTab, false);
        }

        // Add tab selection animations
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                animateTabSelection(tab, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                animateTabSelection(tab, false);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Handle tab reselection if needed
            }
        });
    }

    private void animateTitleBar(TextView tvTitle, TextView tvSubtitle, ImageButton btnBack, ImageButton btnSettings) {
        // Title animation
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(-50f);
        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Subtitle animation
        tvSubtitle.setAlpha(0f);
        tvSubtitle.setTranslationY(-30f);
        tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(200)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Back button animation
        btnBack.setAlpha(0f);
        btnBack.setScaleX(0.8f);
        btnBack.setScaleY(0.8f);
        btnBack.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        // Settings button animation
        btnSettings.setAlpha(0f);
        btnSettings.setScaleX(0.8f);
        btnSettings.setScaleY(0.8f);
        btnSettings.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(100)
                .setDuration(500)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();
    }

    private void animateTitleBarUltraModern(TextView tvTitle, TextView tvSubtitle, ImageButton btnBack, ImageButton btnSettings) {
        // Enhanced title animation with rotation and scale
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(-80f);
        tvTitle.setScaleX(0.9f);
        tvTitle.setScaleY(0.9f);
        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setStartDelay(100)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    // Add subtle pulse effect
                    tvTitle.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(400)
                            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                            .withEndAction(() -> {
                                tvTitle.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(300)
                                        .start();
                            })
                            .start();
                })
                .start();

        // Enhanced subtitle animation with fade and slide
        tvSubtitle.setAlpha(0f);
        tvSubtitle.setTranslationY(-40f);
        tvSubtitle.setScaleX(0.95f);
        tvSubtitle.setScaleY(0.95f);
        tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(400)
                .setDuration(600)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

        // Enhanced back button animation with rotation
        btnBack.setAlpha(0f);
        btnBack.setScaleX(0.6f);
        btnBack.setScaleY(0.6f);
        btnBack.setRotation(-90f);
        btnBack.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(700)
                .setStartDelay(200)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.0f))
                .start();

        // Enhanced settings button animation with bounce
        btnSettings.setAlpha(0f);
        btnSettings.setScaleX(0.6f);
        btnSettings.setScaleY(0.6f);
        btnSettings.setRotation(90f);
        btnSettings.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setStartDelay(350)
                .setDuration(700)
                .setInterpolator(new android.view.animation.BounceInterpolator())
                .withEndAction(() -> {
                    // Add subtle rotation hint
                    btnSettings.animate()
                            .rotation(15f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator())
                            .withEndAction(() -> {
                                btnSettings.animate()
                                        .rotation(0f)
                                        .setDuration(200)
                                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                        .start();
                            })
                            .start();
                })
                .start();
    }

    private void animateTabSelection(TabLayout.Tab tab, boolean selected) {
        if (tab.view != null) {
            if (selected) {
                // Enhanced tab selection animation
                tab.view.setScaleX(1.15f);
                tab.view.setScaleY(1.15f);
                tab.view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                        .withStartAction(() -> {
                            // Add subtle elevation change
                            tab.view.setElevation(8f);
                        })
                        .start();
                
                // Add text color animation
                TextView tabText = tab.view.findViewById(android.R.id.text1);
                if (tabText != null) {
                    ObjectAnimator colorAnim = ObjectAnimator.ofArgb(tabText, "textColor", 
                        getResources().getColor(android.R.color.white), 
                        getResources().getColor(android.R.color.white));
                    colorAnim.setDuration(300);
                    colorAnim.start();
                }
            } else {
                // Reset elevation when unselected
                tab.view.setElevation(0f);
            }
        }
    }

    private static class MedicalRecordPagerAdapter extends FragmentStateAdapter {

        public MedicalRecordPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new MedicalRecordOverviewFragmentNew();
                case 1:
                    return new ConsultationRecordsFragmentNew();
                case 2:
                    return new TestReportListFragmentNew();
                case 3:
                    return new EmotionDiaryFragmentNew();
                case 4:
                    return new HealthMetricsFragmentNew();
                default:
                    return new MedicalRecordOverviewFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
