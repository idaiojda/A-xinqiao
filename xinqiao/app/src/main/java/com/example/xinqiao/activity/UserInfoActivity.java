package com.example.xinqiao.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import android.view.KeyEvent;
import android.content.Context;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.AvatarAdapter;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.utils.AvatarUtils;
import com.example.xinqiao.utils.ImageUtils;
import de.hdodenhof.circleimageview.CircleImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import android.Manifest;
import android.content.pm.PackageManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import androidx.core.content.FileProvider;
import java.io.IOException;

public class UserInfoActivity extends AppCompatActivity {
    private CircleImageView ivAvatar;
    private ImageButton btnBack;
    private Uri selectedImageUri;
    private DBUtils dbUtils;
    private MySQLHelper dbHelper;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1001;
    private int currentUserId; // 用于存储当前用户的ID
    private long lastNicknameUpdate;
    private String originalNickname; // 用于存储原始昵称，以便判断昵称是否修改

    // 新增的成员变量，用于编辑功能
    private EditText etNickname;
    private RadioGroup rgGender;
    private TextView tvBirthdayValue; // 用于显示生日的TextView
    private Spinner spMaritalStatus;
    private Spinner spOccupation;
    private EditText etIntroduction;
    private TextView tvSave;
    private LinearLayout layoutAvatar; // 重新添加layoutAvatar，因为头像的点击区域仍然是一个LinearLayout

    private static final int REQUEST_IMAGE_CAPTURE = 1; // 定义拍照请求码
    private static final int REQUEST_IMAGE_CROP = 3; // 定义裁剪请求码

    private Uri cameraPhotoUri; // 用于存储拍照后的URI
    private Uri croppedAvatarUri; // 用于存储裁剪后的URI

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        cropImage(selectedImageUri); // 确保图片经过裁剪流程
                    } else {
                        Toast.makeText(this, "选择图片失败", Toast.LENGTH_SHORT).show();
                        android.util.Log.e("UserInfoActivity", "从相册选择图片但URI为空");
                    }
                }
            });

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraPhotoUri != null) {
                        cropImage(cameraPhotoUri);
                    } else {
                        Toast.makeText(this, "拍照失败", Toast.LENGTH_SHORT).show();
                        android.util.Log.e("UserInfoActivity", "拍照后cameraPhotoUri为空");
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (croppedAvatarUri != null) {
                        try {
                            // 使用ImageUtils加载并缩放图片，避免OOM
                Bitmap bitmap = com.example.xinqiao.utils.ImageUtils.loadAndResizeBitmap(this, croppedAvatarUri);
                            if (bitmap != null) {
                                ivAvatar.setImageBitmap(bitmap);
                                ivAvatar.postInvalidate();
                                avatarChanged = true;
                                selectedAvatarUri = croppedAvatarUri;
                            } else {
                                Toast.makeText(this, "头像预览失败", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "头像预览失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "裁剪图片失败", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == RESULT_CANCELED) {
                    Toast.makeText(this, "裁剪操作已取消", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "裁剪图片失败", Toast.LENGTH_SHORT).show();
                }
            });

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean avatarChanged = false;
    private Uri selectedAvatarUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DBUtils.init(this, new DBUtils.InitCallback() {
            @Override
            public void onSuccess() {
                try {
                    dbUtils = DBUtils.getInstance(UserInfoActivity.this);
                    dbHelper = MySQLHelper.getInstance();
                    currentUserId = AnalysisUtils.readUserId(UserInfoActivity.this);
                    initViews();
                    setListeners();
                    loadUserInfo();
                    loadUserAvatar();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Toast.makeText(UserInfoActivity.this, "数据库连接失败，请稍后重试", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(SQLException e) {
                e.printStackTrace();
                Toast.makeText(UserInfoActivity.this, "数据库连接失败，请稍后重试", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        btnBack = findViewById(R.id.btn_back);
        tvSave = findViewById(R.id.tv_save);

        // 初始化新的编辑控件
        etNickname = findViewById(R.id.et_nickname);
        rgGender = findViewById(R.id.rg_gender);
        tvBirthdayValue = findViewById(R.id.tv_birthday_value);
        spMaritalStatus = findViewById(R.id.sp_marital_status);
        spOccupation = findViewById(R.id.sp_occupation);
        etIntroduction = findViewById(R.id.et_introduction);

        layoutAvatar = findViewById(R.id.layout_avatar); // 重新初始化layoutAvatar

        // 设置婚姻状态选项
        String[] maritalStatusOptions = {"单身", "恋爱", "已婚"};
        ArrayAdapter<String> maritalAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, maritalStatusOptions);
        maritalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMaritalStatus.setAdapter(maritalAdapter);

        // 设置职业选项
        String[] occupationOptions = {"学生党", "上班族", "全职父母", "商人"};
        ArrayAdapter<String> occupationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, occupationOptions);
        occupationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOccupation.setAdapter(occupationAdapter);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> finish());
        tvSave.setOnClickListener(v -> {
            if (avatarChanged && selectedAvatarUri != null) {
                updateUserAvatar(selectedAvatarUri, this::updateUserInfoAndFinish);
            } else {
                updateUserInfoAndFinish();
            }
        });
        layoutAvatar.setOnClickListener(v -> checkAndOpenImagePicker());
        tvBirthdayValue.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
                    tvBirthdayValue.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void loadUserInfo() {
        executor.execute(() -> {
            try {
                Connection conn = dbHelper.getConnection();
                if (conn != null) {
                    try {
                        String sql = "SELECT nickname, gender, birthday, marital_status, occupation, introduction FROM user_info WHERE user_id = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, currentUserId);
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            final String nickname = rs.getString("nickname");
                            final String gender = rs.getString("gender");
                            final java.sql.Date birthday = rs.getDate("birthday");
                            final String maritalStatus = rs.getString("marital_status");
                            final String occupation = rs.getString("occupation");
                            final String introduction = rs.getString("introduction");

                            runOnUiThread(() -> {
                                etNickname.setText(nickname != null ? nickname : "");

                                if ("男".equals(gender)) {
                                    rgGender.check(R.id.rb_male);
                                } else if ("女".equals(gender)) {
                                    rgGender.check(R.id.rb_female);
                                } else {
                                    // 默认选择男性或不选择
                                    rgGender.clearCheck(); // 清除之前的选择
                                    // 或者设置默认值，例如：rgGender.check(R.id.rb_male);
                                }

                                if (birthday != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    tvBirthdayValue.setText(sdf.format(new Date(birthday.getTime())));
                                } else {
                                    tvBirthdayValue.setText("请选择生日");
                                }

                                String[] maritalStatusArray = getResources().getStringArray(R.array.marital_status);
                                int maritalStatusPosition = getPositionFromArray(maritalStatus, maritalStatusArray);
                                if (maritalStatusPosition != -1) {
                                    spMaritalStatus.setSelection(maritalStatusPosition);
                                } else {
                                    spMaritalStatus.setSelection(0); // 默认选择第一个
                                }

                                String[] occupationArray = getResources().getStringArray(R.array.occupation);
                                int occupationPosition = getPositionFromArray(occupation, occupationArray);
                                if (occupationPosition != -1) {
                                    spOccupation.setSelection(occupationPosition);
                                } else {
                                    spOccupation.setSelection(0); // 默认选择第一个
                                }

                                etIntroduction.setText(introduction != null ? introduction : "");

                                android.util.Log.d("UserInfoActivity", "用户信息加载成功: " + nickname);
                            });
                        } else {
                            runOnUiThread(() -> {
                                android.util.Log.d("UserInfoActivity", "未找到用户ID为 " + currentUserId + " 的信息，使用默认值。");
                                // 可以设置默认值或清空字段
                                etNickname.setText("");
                                rgGender.clearCheck(); // 清空选择
                                tvBirthdayValue.setText("请选择生日");
                                spMaritalStatus.setSelection(0);
                                spOccupation.setSelection(0);
                                etIntroduction.setText("");
                            });
                        }
                    } finally {
                        dbHelper.releaseConnection(conn);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(UserInfoActivity.this, "数据库连接失败，无法加载用户信息", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                android.util.Log.e("UserInfoActivity", "加载用户信息失败: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(UserInfoActivity.this, "加载用户信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private int getPositionFromArray(String value, String[] array) {
        if (value == null || array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private void updateUserInfoAndFinish() {
        executor.execute(() -> {
            try {
                Connection conn = dbHelper.getConnection();
                if (conn != null) {
                    try {
                        String sql = "UPDATE user_info SET nickname=?, gender=?, birthday=?, marital_status=?, occupation=?, introduction=?, updated_at=NOW() WHERE user_id=?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        String nickname = etNickname.getText().toString().trim();
                        String gender = rgGender.getCheckedRadioButtonId() == R.id.rb_male ? "男" : "女";
                        String birthdayStr = tvBirthdayValue.getText().toString();
                        String maritalStatus = spMaritalStatus.getSelectedItem().toString();
                        String occupation = spOccupation.getSelectedItem().toString();
                        String introduction = etIntroduction.getText().toString().trim();
                        stmt.setString(1, nickname);
                        stmt.setString(2, gender);
                        if (birthdayStr != null && !birthdayStr.isEmpty() && !birthdayStr.equals("请选择生日")) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date birthday = sdf.parse(birthdayStr);
                                stmt.setDate(3, new java.sql.Date(birthday.getTime()));
                            } catch (Exception e) {
                                stmt.setNull(3, java.sql.Types.DATE);
                            }
                        } else {
                            stmt.setNull(3, java.sql.Types.DATE);
                        }
                        stmt.setString(4, maritalStatus);
                        stmt.setString(5, occupation);
                        stmt.setString(6, introduction);
                        stmt.setInt(7, currentUserId);
                        stmt.executeUpdate();
                        runOnUiThread(() -> {
                            Intent intent = new Intent();
                            intent.putExtra("avatar_updated", true);
                            setResult(RESULT_OK, intent);
                            finish();
                        });
                    } finally {
                        dbHelper.releaseConnection(conn);
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void checkAndOpenImagePicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
                return;
            }
        }
        openImagePicker();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "请授予存储权限以选择头像", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_avatar_selector, null);
        android.widget.GridView gridView = dialogView.findViewById(R.id.gv_default_avatars);
        Button btnPickFromGallery = dialogView.findViewById(R.id.btn_gallery);

        int[] defaultAvatars = AvatarUtils.getDefaultAvatars();
        AvatarAdapter adapter = new AvatarAdapter(this, defaultAvatars);
        gridView.setAdapter(adapter);

        AlertDialog dialog = builder.setView(dialogView).create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Glide.with(this)
                .load(defaultAvatars[position])
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivAvatar);
            selectedImageUri = Uri.parse("android.resource://" + getPackageName() + "/" + defaultAvatars[position]);
            updateUserAvatar(selectedImageUri, null);
            dialog.dismiss();
        });

        btnPickFromGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImage.launch(intent);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadUserAvatar() {
        executor.execute(() -> {
            try {
                Connection conn = dbHelper.getConnection();
                if (conn != null) {
                    try {
                        String sql = "SELECT avatar FROM user_info WHERE user_id = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, currentUserId);
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            android.util.Log.d("UserInfoActivity", "loadUserAvatar: rs.next() 为 true，找到用户记录");
                            byte[] avatarData = rs.getBytes("avatar");
                            
                            if (avatarData == null) {
                                android.util.Log.d("UserInfoActivity", "loadUserAvatar: 从数据库获取的头像数据为 NULL");
                            } else {
                                android.util.Log.d("UserInfoActivity", "loadUserAvatar: 从数据库获取的头像数据大小: " + avatarData.length + " bytes");
                            }

                            if (avatarData != null && avatarData.length > 0) {
                                // 使用BitmapFactory.Options设置采样率，避免OOM
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // 降低采样率，减少内存使用
                options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用RGB_565配置减少内存使用
                final Bitmap bitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.length, options);
                                runOnUiThread(() -> {
                                    if (bitmap != null) {
                                        android.util.Log.d("UserInfoActivity", "头像数据解码成功，设置到ImageView");
                                        ivAvatar.setImageBitmap(bitmap);
                                        ivAvatar.postInvalidate(); // 强制ImageView重新绘制
                                    } else {
                                        android.util.Log.e("UserInfoActivity", "头像数据解码失败，显示默认头像");
                                        ivAvatar.setImageResource(R.drawable.default_avatar);
                                    }
                                });
                            } else {
                                android.util.Log.d("UserInfoActivity", "数据库中的头像数据为空或无效，显示默认头像");
                                runOnUiThread(() -> {
                                    ivAvatar.setImageResource(R.drawable.default_avatar);
                                });
                            }
                        } else {
                            android.util.Log.d("UserInfoActivity", "loadUserAvatar: rs.next() 为 false，数据库中未找到用户头像记录，显示默认头像");
                            runOnUiThread(() -> {
                                ivAvatar.setImageResource(R.drawable.default_avatar);
                            });
                        }
                    } finally {
                        dbHelper.releaseConnection(conn);
                    }
                } else {
                    android.util.Log.e("UserInfoActivity", "加载头像时数据库连接失败");
                    runOnUiThread(() -> Toast.makeText(UserInfoActivity.this, "加载头像失败: 数据库连接失败", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                android.util.Log.e("UserInfoActivity", "加载头像失败: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    ivAvatar.setImageResource(R.drawable.default_avatar);
                    Toast.makeText(UserInfoActivity.this, "加载头像失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    protected long exitTime; //记录第一次点击时的时间

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            // 如果当前界面是UserInfoActivity，返回键行为与MainActivity类似
            finish(); // 简单地关闭当前Activity
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean readLoginStatus() {
        // 此方法可能在UserInfoActivity中不再直接使用，但保留以防其他部分需要
        return AnalysisUtils.readUserId(this) != -1;
    }

    private void clearLoginStatus() {
        AnalysisUtils.clearLoginInfo(this);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // 关闭当前Activity
    }

    private void updateUserAvatar(Uri imageUri, Runnable onSuccess) {
        executor.execute(() -> {
            try {
                byte[] imageData = ImageUtils.uriToByteArray(this, imageUri);
                if (imageData == null) {
                    runOnUiThread(() -> Toast.makeText(this, "图片处理失败", Toast.LENGTH_SHORT).show());
                    return;
                }
                Connection conn = dbHelper.getConnection();
                if (conn != null) {
                    try {
                        String checkSql = "SELECT COUNT(*) FROM user_info WHERE user_id = ?";
                        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                        checkStmt.setInt(1, currentUserId);
                        ResultSet rs = checkStmt.executeQuery();
                        rs.next();
                        int count = rs.getInt(1);
                        String sql;
                        if (count == 0) {
                            sql = "INSERT INTO user_info (user_id, username, password, nickname, avatar, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
                        } else {
                            sql = "UPDATE user_info SET avatar = ? WHERE user_id = ?";
                        }
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        if (count == 0) {
                            stmt.setInt(1, currentUserId);
                            stmt.setString(2, "user" + currentUserId);
                            stmt.setString(3, "123456");
                            stmt.setString(4, "用户" + currentUserId);
                            stmt.setBytes(5, imageData);
                        } else {
                            stmt.setBytes(1, imageData);
                            stmt.setInt(2, currentUserId);
                        }
                        int result = stmt.executeUpdate();
                        if (result > 0) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "头像保存成功", Toast.LENGTH_SHORT).show();
                                avatarChanged = false;
                                if (onSuccess != null) onSuccess.run();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "头像保存失败", Toast.LENGTH_SHORT).show());
                        }
                    } finally {
                        dbHelper.releaseConnection(conn);
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "头像保存异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                android.util.Log.e("UserInfoActivity", "创建图片文件失败: " + ex.getMessage());
                return;
            }

            if (photoFile != null) {
                cameraPhotoUri = FileProvider.getUriForFile(this,
                        "com.example.xinqiao.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                takePictureLauncher.launch(takePictureIntent); // 使用Launcher启动
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // 使用外部缓存目录，避免权限问题和清理更方便
        File storageDir = getExternalCacheDir(); 
        if (storageDir == null) {
            throw new IOException("无法获取外部缓存目录");
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void cropImage(Uri sourceUri) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(sourceUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 400); // 调整输出尺寸
            cropIntent.putExtra("outputY", 400);
            cropIntent.putExtra("scale", true); // 允许缩放
            cropIntent.putExtra("return-data", false); // 不返回数据，而是保存到URI

            croppedAvatarUri = Uri.fromFile(new File(getCacheDir(), "cropped_avatar_" + System.currentTimeMillis() + ".jpg"));
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, croppedAvatarUri); // 输出到指定URI
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            cropImageLauncher.launch(cropIntent); // 使用Launcher启动裁剪
        } catch (Exception e) {
            Toast.makeText(this, "裁剪图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e("UserInfoActivity", "裁剪图片异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}