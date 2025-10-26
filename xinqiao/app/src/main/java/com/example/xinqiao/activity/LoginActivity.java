package com.example.xinqiao.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.utils.PhoneUtils;
import com.google.android.material.textfield.TextInputEditText;
import java.sql.SQLException;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private CheckBox cbRememberPassword;
    private Button btnLogin;
    private Button btnRegister;
    private LinearLayout logoContainer;
    private View loginCard;
    private SharedPreferences sp;
    private DBUtils dbUtils;
    private static final String SP_NAME = "login_info";
    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        initData();
        setListeners();
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbRememberPassword = findViewById(R.id.cb_remember_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        logoContainer = findViewById(R.id.logo_container);
        loginCard = findViewById(R.id.login_card);
        
        // 禁用登录按钮，直到数据库初始化完成
        btnLogin.setEnabled(false);
        
        // 启动动画
        startAnimations();
        
        // 异步初始化数据库
        DBUtils.init(this, new DBUtils.InitCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    try {
                        dbUtils = DBUtils.getInstance(LoginActivity.this);
                        btnLogin.setEnabled(true);
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
        Toast.makeText(this, "数据库连接失败，请稍后重试", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        View tvForgetPassword = findViewById(R.id.tv_forget_password);
        tvForgetPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, FindPasswordActivity.class)));
    }
    
    private void startAnimations() {
        // Logo淡入动画
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logoContainer.startAnimation(fadeIn);
        
        // 登录卡片从下方滑入
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp.setStartOffset(300); // 延迟300ms开始
        loginCard.startAnimation(slideUp);
    }
    


    private void initData() {
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        boolean isRemember = sp.getBoolean("remember_password", false);
        if (isRemember) {
            String username = sp.getString("username", "");
            String password = sp.getString("password", "");
            etUsername.setText(username);
            etPassword.setText(password);
            cbRememberPassword.setChecked(true);
        }
    }

    private void attemptLogin() {
        String phone = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 验证手机号格式
        if (!PhoneUtils.isValidPhoneNumber(phone)) {
            Toast.makeText(this, getString(R.string.phone_format_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        btnLogin.setEnabled(false);
        
        // Validate user login in background thread
        new LoginTask(phone, password).execute();
    }

    private class LoginTask extends AsyncTask<Void, Void, Integer> {
        private final String phone;
        private final String password;

        LoginTask(String phone, String password) {
            this.phone = phone;
            this.password = password;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return dbUtils.validateUser(phone, password);
        }

        @Override
        protected void onPostExecute(Integer userId) {
            btnLogin.setEnabled(true);
            
            if (userId != -1) { // 登录成功
                saveRememberPasswordInfo(phone, password); // 保存记住密码信息
                loginSuccess(phone, userId); // 登录成功，并传递用户ID
            } else { // 登录失败
                Toast.makeText(LoginActivity.this, getString(R.string.phone_not_registered), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveRememberPasswordInfo(String phone, String password) {
        SharedPreferences.Editor editor = sp.edit();
        if (cbRememberPassword.isChecked()) {
            editor.putBoolean("remember_password", true);
            editor.putString("username", phone); // 保持键名不变，但存储的是手机号
            editor.putString("password", password);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void loginSuccess(String phone, int userId) {
        // 设置登录状态并保存手机号和用户ID
        AnalysisUtils.saveLoginInfo(this, phone, userId);
        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
        // 登录成功后跳转回主界面并清空任务栈
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
