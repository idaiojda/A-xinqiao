package com.example.xinqiao.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.View;
import androidx.appcompat.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.os.LocaleListCompat;

import com.example.xinqiao.R;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.community.SettingsRepository;

    public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout itemPrivacy = findViewById(R.id.item_privacy_mode);
        TextView subtitlePrivacy = findViewById(R.id.subtitle_privacy_mode);
        LinearLayout itemDeactivation = findViewById(R.id.item_deactivation);
        LinearLayout itemEditProfile = findViewById(R.id.item_edit_profile);
        LinearLayout itemAuthorization = findViewById(R.id.item_authorization);
        LinearLayout itemAbout = findViewById(R.id.item_about);
        LinearLayout itemLanguage = findViewById(R.id.item_language);
        TextView subtitleLanguage = findViewById(R.id.subtitle_language);
        Switch switchTwoFactor = findViewById(R.id.switch_two_factor);
        Switch switchNotifications = findViewById(R.id.switch_notifications);
        Button btnLogout = findViewById(R.id.btn_logout);

        // 设置沉浸式状态栏与顶部栏背景色一致
        getWindow().setStatusBarColor(getResources().getColor(R.color.settings_card_bg));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        SettingsRepository.INSTANCE.init(getApplicationContext());
        String userName = AnalysisUtils.readLoginUserName(this);
        SettingsRepository.INSTANCE.get(userName, setting -> {
            runOnUiThread(() -> {
                String privacy = setting != null && "anonymous".equals(setting.getPrivacyMode()) ? getString(R.string.privacy_mode_anonymous) : getString(R.string.privacy_mode_partial);
                subtitlePrivacy.setText(privacy);
                switchTwoFactor.setChecked(setting != null && setting.getTwoFactorEnabled());
                switchNotifications.setChecked(setting != null && setting.getNotificationsEnabled());
                String langCode = setting != null ? setting.getAppLanguage() : "zh";
                subtitleLanguage.setText("en".equals(langCode) ? getString(R.string.language_en) : getString(R.string.language_zh));
            });
            return kotlin.Unit.INSTANCE;
        });

        itemPrivacy.setOnClickListener(v -> {
            Intent i = new Intent(SettingActivity.this, PrivacySettingActivity.class);
            startActivity(i);
        });
        itemDeactivation.setOnClickListener(v -> {
            Intent i = new Intent(SettingActivity.this, AccountDeactivationActivity.class);
            startActivity(i);
        });
        itemEditProfile.setOnClickListener(v -> {
            Intent i = new Intent(SettingActivity.this, UserInfoActivity.class);
            startActivity(i);
        });
        itemAuthorization.setOnClickListener(v -> {
            Intent i = new Intent(SettingActivity.this, AuthorizationActivity.class);
            startActivity(i);
        });
        itemAbout.setOnClickListener(v -> {
            Intent i = new Intent(SettingActivity.this, AboutActivity.class);
            startActivity(i);
        });
        itemLanguage.setOnClickListener(v -> {
            String[] options = new String[]{getString(R.string.language_zh), getString(R.string.language_en)};
            new AlertDialog.Builder(SettingActivity.this)
                    .setTitle(getString(R.string.language))
                    .setItems(options, (dialog, which) -> {
                        String lang = which == 1 ? "en" : "zh";
                        SettingsRepository.INSTANCE.updateLanguage(userName, lang);
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
                        subtitleLanguage.setText(which == 1 ? getString(R.string.language_en) : getString(R.string.language_zh));
                        recreate();
                    })
                    .show();
        });

        switchTwoFactor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsRepository.INSTANCE.updateTwoFactor(userName, isChecked);
        });
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsRepository.INSTANCE.updateNotifications(userName, isChecked);
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.SharedPreferences spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE);
                spLogin.edit().remove("auth_token").apply();
                AnalysisUtils.clearLoginInfo(SettingActivity.this);
                Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            String userName = AnalysisUtils.readLoginUserName(this);
            String cached = SettingsRepository.INSTANCE.getPrivacyModeCached(getApplicationContext(), userName);
            TextView subtitlePrivacy = findViewById(R.id.subtitle_privacy_mode);
            if (subtitlePrivacy != null) {
                subtitlePrivacy.setText("anonymous".equals(cached) ? getString(R.string.privacy_mode_anonymous) : getString(R.string.privacy_mode_partial));
            }
            // 再次异步拉取最新设置，确保最终一致
            SettingsRepository.INSTANCE.get(userName, setting -> {
                runOnUiThread(() -> {
                    if (setting != null && subtitlePrivacy != null) {
                        subtitlePrivacy.setText("anonymous".equals(setting.getPrivacyMode()) ? getString(R.string.privacy_mode_anonymous) : getString(R.string.privacy_mode_partial));
                    }
                });
                return kotlin.Unit.INSTANCE;
            });
        } catch (Exception ignored) {}
    }
}
