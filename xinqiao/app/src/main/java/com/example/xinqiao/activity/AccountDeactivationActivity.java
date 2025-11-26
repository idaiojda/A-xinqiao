package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.xinqiao.R;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.util.AnalysisUtils;

import java.sql.Connection;

public class AccountDeactivationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_deactivation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set immersive status bar
        getWindow().setStatusBarColor(getResources().getColor(R.color.deactivation_card_bg));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ImageButton btnBack = findViewById(R.id.btn_back);
        Button btnSubmit = findViewById(R.id.btn_submit_deactivation);
        TextView tvHint = findViewById(R.id.tv_hint);

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String userName = AnalysisUtils.readLoginUserName(this);
            
            new AlertDialog.Builder(this)
                    .setTitle(R.string.deactivation_confirm_title)
                    .setMessage(R.string.deactivation_confirm_message)
                    .setPositiveButton(R.string.deactivation_confirm_submit, (dialog, which) -> {
                        processDeactivationRequest(userName);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void processDeactivationRequest(String userName) {
        try {
            Connection conn = MySQLHelper.getInstance().getConnection();
            if (conn != null) {
                try {
                    conn.createStatement().execute("CREATE TABLE IF NOT EXISTS deactivation_requests (user_name VARCHAR(64) PRIMARY KEY, requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                    java.sql.PreparedStatement stmt = conn.prepareStatement("REPLACE INTO deactivation_requests (user_name) VALUES (?)");
                    stmt.setString(1, userName != null ? userName : "");
                    stmt.executeUpdate();
                } finally {
                    MySQLHelper.getInstance().releaseConnection(conn);
                }
            }
        } catch (Exception ignored) {}
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.deactivation_submitted_title)
                .setMessage(getString(R.string.deactivation_requested))
                .setPositiveButton(R.string.confirm, (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}
