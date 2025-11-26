package com.example.xinqiao.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.xinqiao.R;

/**
 * Custom chip component with enhanced animations and interactions
 */
public class EnhancedChip extends FrameLayout {
    
    private TextView textView;
    private ImageView iconView;
    private boolean isChecked = false;
    private OnCheckedChangeListener listener;
    
    public interface OnCheckedChangeListener {
        void onCheckedChanged(EnhancedChip chip, boolean isChecked);
    }
    
    public EnhancedChip(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public EnhancedChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public EnhancedChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.component_enhanced_chip, this, true);
        
        textView = findViewById(R.id.tv_chip_text);
        iconView = findViewById(R.id.iv_chip_icon);
        
        setClickable(true);
        setFocusable(true);
        
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
        
        updateVisualState();
    }
    
    public void setText(String text) {
        textView.setText(text);
    }
    
    public String getText() {
        return textView.getText().toString();
    }
    
    public void setIcon(int iconResId) {
        iconView.setImageResource(iconResId);
    }
    
    public void setChecked(boolean checked) {
        if (isChecked != checked) {
            isChecked = checked;
            updateVisualState();
            if (listener != null) {
                listener.onCheckedChanged(this, isChecked);
            }
        }
    }
    
    public boolean isChecked() {
        return isChecked;
    }
    
    public void toggle() {
        setChecked(!isChecked);
    }
    
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }
    
    private void updateVisualState() {
        if (isChecked) {
            animateToCheckedState();
        } else {
            animateToUncheckedState();
        }
    }
    
    private void animateToCheckedState() {
        // Scale animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.05f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.05f);
        
        // Background color animation
        ObjectAnimator backgroundColor = ObjectAnimator.ofArgb(this, "backgroundColor", 
            getResources().getColor(R.color.chip_background), 
            getResources().getColor(R.color.colorPrimary));
        
        // Text color animation
        ObjectAnimator textColor = ObjectAnimator.ofArgb(textView, "textColor",
            getResources().getColor(R.color.settings_title),
            getResources().getColor(R.color.white));
        
        // Icon visibility and rotation
        iconView.setVisibility(VISIBLE);
        ObjectAnimator iconRotation = ObjectAnimator.ofFloat(iconView, "rotation", 0f, 360f);
        ObjectAnimator iconAlpha = ObjectAnimator.ofFloat(iconView, "alpha", 0f, 1f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, backgroundColor, textColor, iconRotation, iconAlpha);
        animatorSet.setDuration(200);
        animatorSet.start();
        
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset to original scale to avoid layout issues
                setScaleX(1f);
                setScaleY(1f);
            }
        });
    }
    
    private void animateToUncheckedState() {
        // Scale animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.95f);
        
        // Background color animation
        ObjectAnimator backgroundColor = ObjectAnimator.ofArgb(this, "backgroundColor",
            getResources().getColor(R.color.colorPrimary),
            getResources().getColor(R.color.chip_background));
        
        // Text color animation
        ObjectAnimator textColor = ObjectAnimator.ofArgb(textView, "textColor",
            getResources().getColor(R.color.white),
            getResources().getColor(R.color.settings_title));
        
        // Icon animations
        ObjectAnimator iconRotation = ObjectAnimator.ofFloat(iconView, "rotation", 360f, 0f);
        ObjectAnimator iconAlpha = ObjectAnimator.ofFloat(iconView, "alpha", 1f, 0f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, backgroundColor, textColor, iconRotation, iconAlpha);
        animatorSet.setDuration(200);
        animatorSet.start();
        
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                iconView.setVisibility(GONE);
                // Reset to original scale
                setScaleX(1f);
                setScaleY(1f);
            }
        });
    }
}