package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.AuthorizationAdapter;
import com.example.xinqiao.repository.MedicalRecordRepository;
import com.example.xinqiao.room.entities.AuthorizationEntity;
import com.example.xinqiao.util.AnalysisUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class AuthorizationActivity extends AppCompatActivity {

    private AuthorizationAdapter adapter;
    private MedicalRecordRepository repo;
    private String userName;
    private TextView tvSummary;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);

        TextView tvTitle = findViewById(R.id.tv_title);
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvSummary = findViewById(R.id.tv_summary);
        RecyclerView rv = findViewById(R.id.rv_authorizations);
        View btnAdd = findViewById(R.id.btn_add_auth);
        View btnRevoke = findViewById(R.id.btn_revoke_auth);

        tvTitle.setText("授权管理");
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        userName = AnalysisUtils.readLoginUserName(this);
        repo = new MedicalRecordRepository(this);

        adapter = new AuthorizationAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        loadAuthorizations();

        btnAdd.setOnClickListener(v -> showAddDialog());
        btnRevoke.setOnClickListener(v -> showRevokeDialog());
    }

    private void loadAuthorizations() {
        List<AuthorizationEntity> list = repo.getAuthorizations(userName != null ? userName : "", false);
        adapter.setItems(list);
        tvSummary.setText(list == null || list.isEmpty() ? "暂无授权记录" : ("当前授权数量：" + list.size()));
    }

    private void showAddDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_add_authorization, null);
        EditText etName = content.findViewById(R.id.et_counselor_name);
        CheckBox cbConsult = content.findViewById(R.id.cb_consult);
        CheckBox cbTest = content.findViewById(R.id.cb_test);
        CheckBox cbDiary = content.findViewById(R.id.cb_diary);
        RadioGroup rg = content.findViewById(R.id.rg_duration);
        RadioButton rb1 = content.findViewById(R.id.rb_1);
        RadioButton rb3 = content.findViewById(R.id.rb_3);
        RadioButton rb7 = content.findViewById(R.id.rb_7);
        rb3.setChecked(true);

        new AlertDialog.Builder(this)
                .setTitle("新增授权")
                .setView(content)
                .setPositiveButton("确定", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    List<String> scopes = new ArrayList<>();
                    if (cbConsult.isChecked()) scopes.add("consult");
                    if (cbTest.isChecked()) scopes.add("test");
                    if (cbDiary.isChecked()) scopes.add("diary");
                    if (scopes.isEmpty()) scopes = Arrays.asList("consult", "test", "diary");
                    int days = rb1.isChecked() ? 1 : (rb7.isChecked() ? 7 : 3);

                    long now = System.currentTimeMillis();
                    AuthorizationEntity e = new AuthorizationEntity();
                    e.userName = userName != null ? userName : "";
                    e.counselorName = name.isEmpty() ? "未命名咨询师" : name;
                    e.scopes = String.join(",", scopes);
                    e.durationDays = days;
                    e.startTimestamp = now;
                    e.endTimestamp = now + days * 24L * 60L * 60L * 1000L;
                    e.status = "已生效";
                    repo.addAuthorization(e);
                    loadAuthorizations();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRevokeDialog() {
        List<AuthorizationEntity> list = adapter.getItems();
        if (list == null || list.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("当前没有授权可撤销")
                    .setPositiveButton("确定", null)
                    .show();
            return;
        }
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            AuthorizationEntity e = list.get(i);
            labels[i] = (e.counselorName != null ? e.counselorName : e.counselorId) + "  (" + (e.scopes != null ? e.scopes : "-") + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("选择要撤销的授权")
                .setItems(labels, (d, which) -> {
                    AuthorizationEntity e = list.get(which);
                    e.status = "已过期";
                    e.endTimestamp = System.currentTimeMillis();
                    repo.updateAuthorization(e);
                    loadAuthorizations();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
