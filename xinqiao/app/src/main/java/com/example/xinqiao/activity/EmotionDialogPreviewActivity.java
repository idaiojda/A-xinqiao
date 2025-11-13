package com.example.xinqiao.activity;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;

public class EmotionDialogPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_emotion_entry);

        SeekBar sbMood = findViewById(R.id.sb_mood);
        TextView tvMoodValue = findViewById(R.id.tv_mood_value);

        tvMoodValue.setText("当前：" + sbMood.getProgress());
        styleMoodChip(tvMoodValue, sbMood.getProgress());

        sbMood.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMoodValue.setText("当前：" + progress);
                styleMoodChip(tvMoodValue, progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void styleMoodChip(TextView tv, int mood) {
        String face;
        int fillColor;
        if (mood <= 3) {
            face = " 😟";
            fillColor = Color.parseColor("#DBEAFE");
        } else if (mood <= 6) {
            face = " 🙂";
            fillColor = Color.parseColor("#DCFCE7");
        } else if (mood <= 8) {
            face = " 😊";
            fillColor = Color.parseColor("#FFEDD5");
        } else {
            face = " 😁";
            fillColor = Color.parseColor("#FEE2E2");
        }
        tv.setText("当前：" + mood + face);
        try {
            if (tv.getBackground() instanceof GradientDrawable) {
                GradientDrawable gd = (GradientDrawable) tv.getBackground().mutate();
                gd.setColor(fillColor);
            }
        } catch (Exception ignore) {}
    }
}
