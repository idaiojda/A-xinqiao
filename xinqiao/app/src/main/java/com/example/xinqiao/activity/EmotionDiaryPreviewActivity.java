package com.example.xinqiao.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;
import com.example.xinqiao.fragment.EmotionDiaryFragment;

public class EmotionDiaryPreviewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_diary_preview);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preview_container, new EmotionDiaryFragment(), "EmotionDiaryFragment")
                .commitAllowingStateLoss();
        }
    }
}

