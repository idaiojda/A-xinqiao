package com.example.xinqiao.activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;

import android.os.Handler;
import android.os.Looper;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private TextView tv_version;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: 开始创建闪屏页");
        setContentView(R.layout.activity_splash);
        //设置此界面为竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        init();
    }
    private void init(){
        Log.d(TAG, "init: 初始化闪屏页");
        tv_version=(TextView) findViewById(R.id.tv_version);
        try {
            //获取程序包信息
            PackageInfo info= getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = info.versionName;
            Log.d(TAG, "init: 版本号: " + versionName);
            tv_version.setText("V"+versionName);
        }catch (PackageManager.NameNotFoundException e){
            Log.e(TAG, "init: 获取版本号失败", e);
            e.printStackTrace();
            tv_version.setText("V");
        }
        //使用Handler在主线程延迟执行跳转
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                Log.d(TAG, "Handler: 开始跳转到主界面");
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }, 3000);
        Log.d(TAG, "init: Handler设置完成，3秒后跳转");
    }
}