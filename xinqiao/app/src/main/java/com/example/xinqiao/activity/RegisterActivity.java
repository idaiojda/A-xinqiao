package com.example.xinqiao.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.activity.EdgeToEdge;

import com.bumptech.glide.Glide;
import com.example.xinqiao.R;
import com.example.xinqiao.adapter.AvatarAdapter;
import com.example.xinqiao.bean.UserBean;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.utils.AvatarUtils;
import com.example.xinqiao.utils.ImageUtils;
import com.example.xinqiao.utils.PhoneUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.example.xinqiao.room.AppDatabase;
import com.example.xinqiao.room.entity.UserInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_IMAGE_CROP = 3;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputEditText etNickname;
    private CircleImageView ivAvatar;
    private Button btnSelectAvatar;
    private Button btnRegister;
    private LinearLayout titleContainer;
    private View registerCard;

    private Uri currentPhotoUri;
    private Bitmap currentAvatar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MySQLHelper dbHelper;
    private DBUtils dbUtils;
    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // 初始化 MySQLHelper
        MySQLHelper.init(this, new MySQLHelper.InitCallback() {
            @Override
            public void onSuccess() {
                dbHelper = MySQLHelper.getInstance();
            }

            @Override
            public void onError(SQLException e) {
                Toast.makeText(RegisterActivity.this, "数据库初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        initViews();
        setListeners();
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etNickname = findViewById(R.id.et_nickname);
        ivAvatar = findViewById(R.id.iv_avatar);
        btnSelectAvatar = findViewById(R.id.btn_select_avatar);
        btnRegister = findViewById(R.id.btn_register);
        titleContainer = findViewById(R.id.title_container);
        registerCard = findViewById(R.id.register_card);

        // 加载默认头像
        Glide.with(this)
            .load(R.drawable.default_avatar)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .circleCrop()
            .into(ivAvatar);
            
        // 启动动画
        startAnimations();
    }

    private void setListeners() {
        btnSelectAvatar.setOnClickListener(v -> {
            // 添加按钮点击动画
            Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
            btnSelectAvatar.startAnimation(scaleIn);
            showImagePickerDialog();
        });
        btnRegister.setOnClickListener(v -> register());
    }
    
    private void startAnimations() {
        // 标题区域淡入动画
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        titleContainer.startAnimation(fadeIn);
        
        // 注册卡片从下方滑入
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideUp.setStartOffset(300); // 延迟300ms开始
        registerCard.startAnimation(slideUp);
    }

    private void showImagePickerDialog() {
        String[] options = {"拍照", "从相册选择"};
        new AlertDialog.Builder(this)
                .setTitle("选择图片来源")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (checkCameraPermission()) {
                            takePicture();
                        }
                    } else {
                        if (checkStoragePermission()) {
                            pickImage();
                        }
                    }
                })
                .show();
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
    


    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        "com.example.xinqiao.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void pickImage() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK);
    }

    private void cropImage(Uri sourceUri) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(sourceUri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        cropIntent.putExtra("outputX", 200);
        cropIntent.putExtra("outputY", 200);
        cropIntent.putExtra("return-data", true);
        
        // 添加输出URI
        File croppedFile = new File(getCacheDir(), "cropped_avatar.jpg");
        Uri croppedUri = FileProvider.getUriForFile(this,
                "com.example.xinqiao.fileprovider",
                croppedFile);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, croppedUri);
        
        startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    if (currentPhotoUri != null) {
                        cropImage(currentPhotoUri);
                    }
                    break;
                case REQUEST_IMAGE_PICK:
                    if (data != null) {
                        currentPhotoUri = data.getData();
                        cropImage(currentPhotoUri);
                    }
                    break;
                case REQUEST_IMAGE_CROP:
                    if (data != null && data.getExtras() != null) {
                        currentAvatar = data.getExtras().getParcelable("data");
                        if (currentAvatar != null) {
                            Glide.with(this)
                                .load(currentAvatar)
                                .circleCrop()
                                .into(ivAvatar);
                        }
                    }
                    break;
            }
        }
    }

    private void register() {
        String phone = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String nickname = etNickname.getText() != null ? etNickname.getText().toString().trim() : "";

        // 验证手机号格式
        if (!PhoneUtils.isValidPhoneNumber(phone)) {
            Toast.makeText(this, getString(R.string.phone_format_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "密码和确认密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
            return;
        }

        // 如果未选择头像，自动分配一个默认头像
        if (currentAvatar == null) {
            int[] defaultAvatars = AvatarUtils.getDefaultAvatars();
            int randomIndex = new Random().nextInt(defaultAvatars.length);
            int avatarResId = defaultAvatars[randomIndex];
            // 使用BitmapUtils加载并缩放图片，避免OOM
            currentAvatar = com.example.xinqiao.utils.BitmapUtils.decodeSampledBitmapFromResource(
                    getResources(), avatarResId, 200, 200);
            // UI上也显示该头像
            Glide.with(this)
                .load(avatarResId)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .circleCrop()
                .into(ivAvatar);
        }

        // Show loading indicator
        btnRegister.setEnabled(false);

        // 保存用户信息到数据库
        UserBean userBean = new UserBean();
        userBean.userName = phone; // 使用手机号作为用户名
        userBean.password = password;
        userBean.nickName = nickname;
        userBean.signature = "";
        userBean.sex = "保密";
        userBean.avatarPath = currentAvatar.toString();
        
        // Register user in background thread
        executor.execute(() -> {
            Connection conn = null;
            try {
                conn = dbHelper.getConnection();
                if (conn == null) {
                    boolean localOk = registerLocalWithRoom(phone, password, nickname);
                    runOnUiThread(() -> {
                        if (localOk) {
                            Toast.makeText(RegisterActivity.this, "注册成功（本地）", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "注册失败（本地）", Toast.LENGTH_SHORT).show();
                            btnRegister.setEnabled(true);
                        }
                    });
                    return;
                }

                // 检查手机号是否已存在
                String checkSql = "SELECT COUNT(*) FROM user_info WHERE username = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, phone);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);
                rs.close();
                checkStmt.close();

                if (count > 0) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, getString(R.string.phone_already_exists), Toast.LENGTH_SHORT).show());
                } else {
                    // 转换头像到字节数组
                    byte[] avatarData = null;
                    if (currentPhotoUri != null) {
                        avatarData = ImageUtils.uriToByteArray(this, currentPhotoUri);
                    } else if (currentAvatar != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        currentAvatar.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        avatarData = stream.toByteArray();
                    }
                    
                    // 插入新用户记录到 user_info 表
                    String sql = "INSERT INTO user_info (username, password, nickname, gender, birthday, marital_status, occupation, introduction, avatar, balance, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, phone);
                    stmt.setString(2, password);
                    stmt.setString(3, nickname.isEmpty() ? phone : nickname); // 如果昵称为空，使用手机号
                    stmt.setString(4, "未知"); // 默认性别
                    stmt.setNull(5, java.sql.Types.DATE); // 默认生日为NULL
                    stmt.setString(6, "未设置"); // 默认婚姻状况
                    stmt.setString(7, "未设置"); // 默认职业
                    stmt.setString(8, ""); // 默认简介
                    if (avatarData != null) {
                        stmt.setBytes(9, avatarData);
                    } else {
                        stmt.setNull(9, java.sql.Types.BLOB);
                    }
                    stmt.setDouble(10, 0.00); // 设置初始余额为0

                    int result = stmt.executeUpdate();
                    stmt.close();

                    if (result > 0) {
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show());
                        finish(); // 注册成功后关闭当前Activity
                    } else {
                        boolean localOk = registerLocalWithRoom(phone, password, nickname);
                        runOnUiThread(() -> {
                            if (localOk) {
                                Toast.makeText(RegisterActivity.this, "注册成功（本地）", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this, "注册失败", Toast.LENGTH_SHORT).show();
                                btnRegister.setEnabled(true);
                            }
                        });
                    }
                }
            } catch (SQLException e) {
                android.util.Log.e("RegisterActivity", "注册失败: " + e.getMessage());
                e.printStackTrace();
                boolean localOk = false;
                try {
                    localOk = registerLocalWithRoom(phone, password, nickname);
                } catch (Exception ex) {
                    android.util.Log.e("RegisterActivity", "本地注册异常: " + ex.getMessage());
                }
                boolean finalLocalOk = localOk;
                runOnUiThread(() -> {
                    if (finalLocalOk) {
                        Toast.makeText(RegisterActivity.this, "注册成功（本地）", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "注册失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnRegister.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("RegisterActivity", "处理头像或注册时发生错误: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "处理头像或注册时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                runOnUiThread(() -> btnRegister.setEnabled(true));
            } finally {
                if (conn != null) {
                    dbHelper.releaseConnection(conn);
                }
            }
        });
    }

    /**
     * 使用Room进行本地注册兜底
     */
    private boolean registerLocalWithRoom(String phone, String password, String nickname) {
        try {
            UserInfo existing = AppDatabase.getInstance(this).userInfoDao().getUserByUsername(phone);
            if (existing != null) {
                // 本地已存在相同用户名
                return false;
            }

            // 转换头像到字节数组
            byte[] avatarData = null;
            if (currentPhotoUri != null) {
                avatarData = ImageUtils.uriToByteArray(this, currentPhotoUri);
            } else if (currentAvatar != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                currentAvatar.compress(Bitmap.CompressFormat.PNG, 100, stream);
                avatarData = stream.toByteArray();
            }

            UserInfo user = new UserInfo();
            user.setUsername(phone);
            user.setPassword(password);
            user.setNickname(nickname.isEmpty() ? phone : nickname);
            user.setGender("未知");
            user.setBirthday(null);
            user.setMaritalStatus("未设置");
            user.setOccupation("未设置");
            user.setIntroduction("");
            user.setAvatar(avatarData);
            user.setBalance(0.00);
            user.setCreatedAt(new java.util.Date());
            user.setUpdatedAt(new java.util.Date());

            long rowId = AppDatabase.getInstance(this).userInfoDao().insert(user);
            return rowId > 0;
        } catch (Exception ex) {
            android.util.Log.e("RegisterActivity", "Room本地注册失败: " + ex.getMessage());
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
