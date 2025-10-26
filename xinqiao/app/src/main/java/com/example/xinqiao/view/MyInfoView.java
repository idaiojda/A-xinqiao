package com.example.xinqiao.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.xinqiao.R;
import com.example.xinqiao.activity.*;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.mysql.DBUtils;
import android.util.Base64;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.example.xinqiao.utils.PaymentUtils;
import de.hdodenhof.circleimageview.CircleImageView;

public class MyInfoView extends LinearLayout implements com.example.xinqiao.mysql.WeakReferenceCallback {
    private CircleImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvBalance;
    private LinearLayout ll_head;
    private RelativeLayout rl_course_history, rl_setting, rl_balance;
    private Activity mContext;
    private LayoutInflater mInflater;
    private View mCurrentView;
    private PaymentUtils paymentUtils;

    public MyInfoView(Context context) {
        super(context);
        mContext = (Activity) context;
        mInflater = LayoutInflater.from(mContext);
        init();
    }

    public MyInfoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = (Activity) context;
        mInflater = LayoutInflater.from(mContext);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.main_view_myinfo, this, true);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvNickname = findViewById(R.id.tv_user_name);
        tvBalance = findViewById(R.id.tv_balance);
        ll_head = findViewById(R.id.ll_head);
        rl_course_history = findViewById(R.id.rl_course_history);
        rl_setting = findViewById(R.id.rl_setting);
        rl_balance = findViewById(R.id.rl_balance);
        paymentUtils = new PaymentUtils(mContext);
        mCurrentView = this;
        setLoginParams(readLoginStatus());
        ll_head.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(readLoginStatus()){
                    Intent intent = new Intent(mContext, UserInfoActivity.class);
                    mContext.startActivityForResult(intent, 2);
                }else{
                    Intent intent = new Intent(mContext, LoginActivity.class);
                    mContext.startActivityForResult(intent,1);
                }
            }
        });
        rl_course_history.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(readLoginStatus()){
                    Intent intent = new Intent(mContext, PlayHistoryActivity.class);
                    mContext.startActivity(intent);
                }else{
                    Toast.makeText(mContext, "您还未登录，请先登录", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rl_setting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(readLoginStatus()){
                    Intent intent = new Intent(mContext, SettingActivity.class);
                    mContext.startActivityForResult(intent,1);
                }else{
                    Toast.makeText(mContext, "您还未登录，请先登录", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 加载默认头像
        Glide.with(this)
            .load(R.drawable.default_avatar)
            .circleCrop()
            .into(ivAvatar);
            
        // 设置余额点击事件
        rl_balance.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(readLoginStatus()){
                    Intent intent = new Intent(mContext, RechargeActivity.class);
                    mContext.startActivity(intent);
                }else{
                    Toast.makeText(mContext, "您还未登录，请先登录", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 更新余额显示
        updateBalance();
    }

    public void setUserInfo(String nickname) {
        tvNickname.setText(nickname);
    }

    public void setAvatar(byte[] avatarData) {
        if (avatarData != null && avatarData.length > 0) {
            Glide.with(this)
                .load(avatarData)
                .circleCrop()
                .into(ivAvatar);
        } else {
            Glide.with(this)
                .load(R.drawable.default_avatar)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .circleCrop()
                .into(ivAvatar);
        }
    }

    private void updateBalance() {
        if(readLoginStatus()) {
            String userName = AnalysisUtils.readLoginUserName(mContext);
            paymentUtils.getBalance(userName, new PaymentUtils.PaymentCallback() {
                @Override
                public void onSuccess() {
                    tvBalance.setText(String.format("￥%.2f", paymentUtils.getCurrentBalance()));
                }
                @Override
                public void onError(String message) {
                    tvBalance.setText("￥0.00");
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            tvBalance.setText("￥0.00");
        }
    }

    public void setLoginParams(boolean isLogin){
        if(isLogin){
            // 获取用户昵称并显示
            getUserNickname(new NicknameCallback() {
                @Override
                public void onSuccess(String nickname) {
                    if (nickname != null && !nickname.isEmpty()) {
                        tvNickname.setText(nickname);
                    } else {
                        // 如果没有昵称，显示用户名（手机号）
                        tvNickname.setText(AnalysisUtils.readLoginUserName(mContext));
                    }
                }

                @Override
                public void onError(Exception e) {
                    // 出错时显示用户名（手机号）
                    tvNickname.setText(AnalysisUtils.readLoginUserName(mContext));
                    Toast.makeText(mContext, "获取用户信息失败", Toast.LENGTH_SHORT).show();
                }
            });
            
            updateBalance();
            // 登录后自动刷新头像
            String userName = AnalysisUtils.readLoginUserName(mContext);
            DBUtils dbUtils = null;
            try {
                dbUtils = DBUtils.getInstance(mContext);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, "数据库未初始化，请稍后重试", Toast.LENGTH_SHORT).show();
                Glide.with(getContext())
                    .load(R.drawable.default_avatar)
                    .circleCrop()
                    .into(ivAvatar);
                return;
            }
            if (dbUtils != null) {
                dbUtils.getUserAvatarPath(userName, new DBUtils.AvatarPathCallback() {
                    @Override
                    public void onSuccess(String avatarBase64) {
                        try {
                            if (avatarBase64 != null && avatarBase64.startsWith("data:image")) {
                                try {
                                    // 分割Base64字符串，减少内存使用
                                    String base64 = avatarBase64.substring(avatarBase64.indexOf(",") + 1);
                                    
                                    // 分段处理Base64字符串，避免一次性分配大内存
                                    int chunkSize = 1024 * 1024; // 1MB chunks
                                    byte[] avatarData = null;
                                    
                                    if (base64.length() > chunkSize * 4) { // 如果Base64字符串非常大
                                        // 加载默认头像
                                        loadDefaultAvatar();
                                        return;
                                    } else {
                                        try {
                                            avatarData = Base64.decode(base64, Base64.DEFAULT);
                                        } catch (OutOfMemoryError e) {
                                            // 内存不足，加载默认头像
                                            loadDefaultAvatar();
                                            return;
                                        }
                                    }
                                    
                                    if (avatarData == null || avatarData.length == 0) {
                                        loadDefaultAvatar();
                                        return;
                                    }
                                    
                                    // 先解码图片尺寸
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;
                                    BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length, options);
                                    
                                    // 计算合适的采样率
                                    int targetSize = 200; // 降低目标尺寸
                                    int sampleSize = 1;
                                    if (options.outHeight > targetSize || options.outWidth > targetSize) {
                                        sampleSize = Math.max(
                                            options.outWidth / targetSize,
                                            options.outHeight / targetSize
                                        );
                                    }
                                    
                                    // 使用计算出的采样率重新解码
                                    options = new BitmapFactory.Options();
                                    options.inSampleSize = Math.max(sampleSize, 2); // 确保至少2倍采样
                                    options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用16位图像格式
                                    options.inPurgeable = true;
                                    options.inInputShareable = true;
                                    
                                    Bitmap bitmap = null;
                                    try {
                                        bitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length, options);
                                        
                                        // 释放原始数据
                                        avatarData = null;
                                        System.gc();
                                        
                                        // 如果图片仍然太大，进行缩放
                                        if (bitmap != null && (bitmap.getWidth() > targetSize || bitmap.getHeight() > targetSize)) {
                                            float scale = (float) targetSize / Math.max(bitmap.getWidth(), bitmap.getHeight());
                                            Matrix matrix = new Matrix();
                                            matrix.postScale(scale, scale);
                                            
                                            try {
                                                Bitmap scaledBitmap = Bitmap.createBitmap(
                                                    bitmap,
                                                    0, 0,
                                                    bitmap.getWidth(), bitmap.getHeight(),
                                                    matrix,
                                                    true
                                                );
                                                
                                                // 如果创建了新的缩放位图，释放原始位图
                                                if (scaledBitmap != bitmap) {
                                                    bitmap.recycle();
                                                    bitmap = scaledBitmap;
                                                }
                                            } catch (OutOfMemoryError e) {
                                                // 缩放失败，尝试使用原始位图
                                                if (bitmap != null && !bitmap.isRecycled()) {
                                                    // 继续使用原始位图
                                                } else {
                                                    loadDefaultAvatar();
                                                    return;
                                                }
                                            }
                                        }
                                        
                                        // 使用Glide加载处理后的位图
                                        if (bitmap != null && !bitmap.isRecycled()) {
                                            Glide.with(getContext())
                                                .load(bitmap)
                                                .into(ivAvatar);
                                        } else {
                                            loadDefaultAvatar();
                                        }
                                    } catch (OutOfMemoryError e) {
                                        // 处理内存不足异常
                                        if (bitmap != null && !bitmap.isRecycled()) {
                                            bitmap.recycle();
                                        }
                                        loadDefaultAvatar();
                                    } catch (Exception e) {
                                        // 处理其他异常
                                        if (bitmap != null && !bitmap.isRecycled()) {
                                            bitmap.recycle();
                                        }
                                        loadDefaultAvatar();
                                    }
                                } catch (Exception e) {
                                    // 处理Base64解码异常
                                    loadDefaultAvatar();
                                }
                            } else {
                                // 无效的Base64数据
                                loadDefaultAvatar();
                            }
                        } catch (Exception e) {
                            // 处理所有其他异常
                            loadDefaultAvatar();
                        }
                    }

                    @Override
                    public void onError(java.sql.SQLException e) {
                        // 加载默认头像
                        loadDefaultAvatar();
                    }
                });
            }
        }else{
            tvNickname.setText("点击登录");
            Glide.with(getContext())
                .load(R.drawable.default_avatar)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .circleCrop()
                .into(ivAvatar);
        }
    }

    // 获取用户昵称的接口
    private void getUserNickname(final NicknameCallback callback) {
        new Thread(() -> {
            try {
                String userName = AnalysisUtils.readLoginUserName(mContext);
                
                // 使用新添加的方法获取用户昵称
                DBUtils dbUtils = DBUtils.getInstance(mContext);
                dbUtils.getUserNickname(userName, new DBUtils.UserNicknameCallback() {
                    @Override
                    public void onSuccess(String nickname) {
                        mContext.runOnUiThread(() -> callback.onSuccess(nickname));
                    }

                    @Override
                    public void onError(java.sql.SQLException e) {
                        mContext.runOnUiThread(() -> callback.onError(e));
                    }
                });
            } catch (Exception e) {
                mContext.runOnUiThread(() -> callback.onError(e));
            }
        }).start();
    }

    // 昵称获取回调接口
    private interface NicknameCallback {
        void onSuccess(String nickname);
        void onError(Exception e);
    }

    private boolean readLoginStatus(){
        SharedPreferences sp = mContext.getSharedPreferences("loginInfo", Context.MODE_PRIVATE);
        boolean isLogin = sp.getBoolean("isLogin", false);
        return isLogin;
    }

    public View getView() {
        return mCurrentView;
    }

    public void showView() {
        mCurrentView.setVisibility(View.VISIBLE);
        // 更新用户信息和余额
        setLoginParams(readLoginStatus());
    }

    // 加载默认头像的辅助方法
    private void loadDefaultAvatar() {
        if (ivAvatar == null || getContext() == null) {
            return;
        }
        
        try {
            // 检查Activity是否已销毁
            if (getContext() instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) getContext();
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return; // 如果Activity已销毁或正在销毁，则不加载图片
                }
            }
            
            Glide.with(getContext().getApplicationContext()) // 使用ApplicationContext避免Activity生命周期问题
                .load(R.drawable.default_avatar)
                .into(ivAvatar);
        } catch (Exception e) {
            // 即使加载默认头像失败也不再尝试
            e.printStackTrace();
        }
    }

    @Override
    public boolean isAlive() {
        Context context = getContext();
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            return !(activity.isFinishing() || activity.isDestroyed());
        }
        return context != null;
    }
}