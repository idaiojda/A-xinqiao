package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.xinqiao.BuildConfig;
import com.example.xinqiao.R;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set immersive status bar
        getWindow().setStatusBarColor(getResources().getColor(R.color.about_card_bg));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvName = findViewById(R.id.tv_app_name);
        TextView tvVersion = findViewById(R.id.tv_version);

        btnBack.setOnClickListener(v -> finish());
        tvName.setText(getString(R.string.app_display_name));
        tvVersion.setText(getString(R.string.version_fmt, BuildConfig.VERSION_NAME));
    }
}

