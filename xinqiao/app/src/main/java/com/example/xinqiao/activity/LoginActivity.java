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
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.util.PhoneUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.example.xinqiao.room.AppDatabase;
import com.example.xinqiao.room.entity.UserInfo;
import com.example.xinqiao.network.NetworkConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
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
    private boolean useLocalLoginFallback = false;
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
                        useLocalLoginFallback = false;
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
        Toast.makeText(this, "远程数据库不可用，已切换到本地登录", Toast.LENGTH_SHORT).show();
        useLocalLoginFallback = true;
        btnLogin.setEnabled(true);
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
            try {
                // 优先走后端登录（密码加密校验）
                com.example.xinqiao.network.LoginResp lr = null;
                try { lr = com.example.xinqiao.network.ApiJava.login(phone, password); } catch (Exception ignored) {}
                if (lr != null && lr.getOk() && lr.getToken() != null) {
                    SharedPreferences spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE);
                    spLogin.edit().putString("auth_token", lr.getToken()).apply();
                    // 取 user_id（从统一的 user_info 表）
                    if (dbUtils != null && dbUtils.isDatabaseAvailable()) {
                        int uid = dbUtils.getUserIdByUsername(phone);
                        if (uid != -1) return uid;
                    }
                } else {
                    // 如果后端登录失败，尝试注册再登录
                    try {
                        boolean regOk = com.example.xinqiao.network.ApiJava.register(phone, password);
                        if (regOk) {
                            com.example.xinqiao.network.LoginResp lr2 = com.example.xinqiao.network.ApiJava.login(phone, password);
                            if (lr2 != null && lr2.getOk() && lr2.getToken() != null) {
                                SharedPreferences spLogin2 = getSharedPreferences("loginInfo", MODE_PRIVATE);
                                spLogin2.edit().putString("auth_token", lr2.getToken()).apply();
                                if (dbUtils != null && dbUtils.isDatabaseAvailable()) {
                                    int uid2 = dbUtils.getUserIdByUsername(phone);
                                    if (uid2 != -1) return uid2;
                                }
                            }
                        }
                    } catch (Exception ignoredReg) {}
                }
                // 后端不可用或无法获取user_id时，最后兜底：本地校验
                if (!useLocalLoginFallback && dbUtils != null && dbUtils.isDatabaseAvailable()) {
                    return dbUtils.validateUser(phone, password);
                } else {
                    UserInfo user = AppDatabase.getInstance(LoginActivity.this)
                            .userInfoDao().login(phone, password);
                    return user != null ? user.getUserId() : -1;
                }
            } catch (Exception ex) {
                android.util.Log.e("LoginActivity", "登录校验异常: " + ex.getMessage());
                return -1;
            }
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
        try { com.example.xinqiao.community.GlobalRealtimeReceiver.INSTANCE.onForeground(getApplication()); } catch (Throwable ignored) {}
        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String token = null;
            boolean isCounselor = false;
            try {
                com.example.xinqiao.network.LoginResp lr = com.example.xinqiao.network.ApiJava.login(phone, String.valueOf(etPassword.getText()));
                if (lr != null && lr.getOk() && lr.getToken() != null) {
                    token = lr.getToken();
                    SharedPreferences spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE);
                    spLogin.edit().putString("auth_token", token).apply();
                } else {
                    boolean regOk = com.example.xinqiao.network.ApiJava.register(phone, String.valueOf(etPassword.getText()));
                    if (regOk) {
                        com.example.xinqiao.network.LoginResp lr2 = com.example.xinqiao.network.ApiJava.login(phone, String.valueOf(etPassword.getText()));
                        if (lr2 != null && lr2.getOk() && lr2.getToken() != null) {
                            token = lr2.getToken();
                            SharedPreferences spLogin2 = getSharedPreferences("loginInfo", MODE_PRIVATE);
                            spLogin2.edit().putString("auth_token", token).apply();
                        }
                    }
                }
                com.example.xinqiao.network.MeResp me = com.example.xinqiao.network.ApiJava.me();
                if (me != null) {
                    java.util.List<String> rolesList = me.getRoles() != null ? me.getRoles() : new java.util.ArrayList<>();
                    for (String rr : rolesList) {
                        if (rr != null && rr.equalsIgnoreCase("COUNSELOR")) { isCounselor = true; break; }
                    }
                }
            } catch (Exception ignored) {}
            final boolean finalIsCounselor = isCounselor;
            runOnUiThread(() -> {
                try {
                    SharedPreferences spLogin = getSharedPreferences("loginInfo", MODE_PRIVATE);
                    SharedPreferences.Editor ed = spLogin.edit();
                    ed.putBoolean("isCounselor", finalIsCounselor);
                    ed.apply();
                } catch (Exception ignored3) {}
                // 返回结果给调用方（若使用startActivityForResult）
                Intent result = new Intent();
                result.putExtra("isLogin", true);
                setResult(RESULT_OK, result);
                // 进入端分流首页
                Intent intent = new Intent(LoginActivity.this, finalIsCounselor ? CounselorMainActivity.class : MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
        // 若是从其它页面通过 startActivityForResult 进入登录页，返回结果给调用方
        // 分流跳转逻辑改为在网络请求完成后统一处理
    }
}
