package com.example.xinqiao.activity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.utils.PhoneUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class FindPasswordActivity extends AppCompatActivity {
    private TextInputEditText etPhone, etCode, etNewPassword;
    private Button btnGetCode, btnConfirm;
    private CountDownTimer timer;
    private boolean codeSent = false;
    private String sentCode = null;
    private DBUtils dbUtils;

    // 密码强度正则表达式
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_password);
        
        initViews();
        initDatabase();
    }

    private void initViews() {
        etPhone = findViewById(R.id.et_phone);
        etCode = findViewById(R.id.et_code);
        etNewPassword = findViewById(R.id.et_new_password);
        btnGetCode = findViewById(R.id.btn_get_code);
        btnConfirm = findViewById(R.id.btn_confirm);

        // 设置返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnGetCode.setOnClickListener(v -> onGetCode());
        btnConfirm.setOnClickListener(v -> onConfirm());
    }

    private void initDatabase() {
        // 异步初始化数据库
        DBUtils.init(this, new DBUtils.InitCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    try {
                        dbUtils = DBUtils.getInstance(FindPasswordActivity.this);
                    } catch (SQLException e) {
                        handleDatabaseError(e);
                    }
                });
            }
            
            @Override
            public void onError(SQLException e) {
                runOnUiThread(() -> handleDatabaseError(e));
            }
        });
    }
    
    private void handleDatabaseError(SQLException e) {
        e.printStackTrace();
        Toast.makeText(this, getString(R.string.database_unavailable), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void onGetCode() {
        String phone = etPhone.getText().toString().trim();
        
        // 验证手机号格式
        if (!PhoneUtils.isValidPhoneNumber(phone)) {
            Toast.makeText(this, getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查数据库连接
        if (dbUtils == null || !dbUtils.isDatabaseAvailable()) {
            Toast.makeText(this, getString(R.string.database_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }

        // 生成6位随机验证码
        sentCode = generateVerificationCode();
        codeSent = true;
        
        // 禁用获取验证码按钮并开始倒计时
        btnGetCode.setEnabled(false);
        btnGetCode.setText("60s");
        
        // 显示验证码（实际开发中应该通过短信服务发送）
        Toast.makeText(this, "验证码已发送: " + sentCode, Toast.LENGTH_LONG).show();
        
        // 开始倒计时
        startCountDownTimer();
    }



    private String generateVerificationCode() {
        // 生成6位数字验证码
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append((int) (Math.random() * 10));
        }
        return code.toString();
    }

    private void startCountDownTimer() {
        timer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                btnGetCode.setText((millisUntilFinished / 1000) + "s");
            }
            public void onFinish() {
                btnGetCode.setEnabled(true);
                btnGetCode.setText(getString(R.string.send_again));
            }
        }.start();
    }

    private void onConfirm() {
        String phone = etPhone.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String newPwd = etNewPassword.getText().toString().trim();
        
        // 验证输入
        if (!validateInputs(phone, code, newPwd)) {
            return;
        }

        // 检查数据库连接
        if (dbUtils == null || !dbUtils.isDatabaseAvailable()) {
            Toast.makeText(this, getString(R.string.database_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }

        // 执行密码重置
        performPasswordReset(phone, newPwd);
    }

    private boolean validateInputs(String phone, String code, String newPwd) {
        // 验证手机号
        if (!PhoneUtils.isValidPhoneNumber(phone)) {
            Toast.makeText(this, getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // 验证验证码
        if (!codeSent || TextUtils.isEmpty(code)) {
            Toast.makeText(this, getString(R.string.get_code_first), Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (!code.equals(sentCode)) {
            Toast.makeText(this, getString(R.string.invalid_code), Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // 验证新密码
        if (TextUtils.isEmpty(newPwd) || newPwd.length() < 6) {
            Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // 检查密码强度（可选）
        if (!PASSWORD_PATTERN.matcher(newPwd).matches()) {
            Toast.makeText(this, getString(R.string.password_weak), Toast.LENGTH_LONG).show();
            return false;
        }
        
        return true;
    }

    private void performPasswordReset(String phone, String newPwd) {
        // 在后台线程中执行数据库操作
        new Thread(() -> {
            boolean success = dbUtils.updateUserPasswordByPhone(phone, newPwd);
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(FindPasswordActivity.this, getString(R.string.password_reset_success), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(FindPasswordActivity.this, getString(R.string.password_reset_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
} 