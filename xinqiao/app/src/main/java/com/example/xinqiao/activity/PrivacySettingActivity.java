package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.xinqiao.R;
import com.example.xinqiao.community.SettingsRepository;
import com.example.xinqiao.util.AnalysisUtils;

public class PrivacySettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_privacy_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set status bar color to match top bar
        getWindow().setStatusBarColor(getResources().getColor(R.color.privacy_card_bg));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ImageButton btnBack = findViewById(R.id.btn_back);
        RadioGroup rg = findViewById(R.id.rg_privacy);
        RadioButton rbAnonymous = findViewById(R.id.rb_anonymous);
        RadioButton rbPartial = findViewById(R.id.rb_partial);
        TextView tvDesc = findViewById(R.id.tv_privacy_desc);
        
        // Get the clickable containers
        LinearLayout anonymousContainer = findViewById(R.id.container_anonymous);
        LinearLayout partialContainer = findViewById(R.id.container_partial);

        btnBack.setOnClickListener(v -> finish());

        SettingsRepository.INSTANCE.init(getApplicationContext());
        String userName = AnalysisUtils.readLoginUserName(this);
        SettingsRepository.INSTANCE.get(userName, setting -> {
            runOnUiThread(() -> {
                String mode = setting != null ? setting.getPrivacyMode() : "partial";
                if ("anonymous".equals(mode)) {
                    rbAnonymous.setChecked(true);
                    tvDesc.setText(R.string.privacy_desc_anonymous);
                } else {
                    rbPartial.setChecked(true);
                    tvDesc.setText(R.string.privacy_desc_partial);
                }
            });
            return kotlin.Unit.INSTANCE;
        });

        // Enforce mutual exclusivity since RadioButtons are nested inside containers
        anonymousContainer.setOnClickListener(v -> {
            rbAnonymous.setChecked(true);
            rbPartial.setChecked(false);
            SettingsRepository.INSTANCE.updatePrivacy(userName, "anonymous");
            tvDesc.setText(R.string.privacy_desc_anonymous);
        });
        partialContainer.setOnClickListener(v -> {
            rbPartial.setChecked(true);
            rbAnonymous.setChecked(false);
            SettingsRepository.INSTANCE.updatePrivacy(userName, "partial");
            tvDesc.setText(R.string.privacy_desc_partial);
        });

        rbAnonymous.setOnClickListener(v -> {
            rbAnonymous.setChecked(true);
            rbPartial.setChecked(false);
            SettingsRepository.INSTANCE.updatePrivacy(userName, "anonymous");
            tvDesc.setText(R.string.privacy_desc_anonymous);
        });
        rbPartial.setOnClickListener(v -> {
            rbPartial.setChecked(true);
            rbAnonymous.setChecked(false);
            SettingsRepository.INSTANCE.updatePrivacy(userName, "partial");
            tvDesc.setText(R.string.privacy_desc_partial);
        });
    }
}
