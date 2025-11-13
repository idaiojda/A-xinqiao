package com.example.xinqiao.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.TestRecord;
import com.example.xinqiao.dao.TestRecordDao;

public class OrderQueryActivity extends AppCompatActivity {
    private EditText etReportId;
    private Button btnQuery;
    private TextView tvResult;
    private Button btnAction;
    private TestRecordDao testRecordDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_query);

        etReportId = findViewById(R.id.et_report_id);
        btnQuery = findViewById(R.id.btn_query);
        tvResult = findViewById(R.id.tv_result);
        btnAction = findViewById(R.id.btn_action);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        testRecordDao = new TestRecordDao(this);

        btnQuery.setOnClickListener(v -> doQuery());
        btnAction.setOnClickListener(v -> {
            Object tag = btnAction.getTag();
            if (tag instanceof Action) {
                handleAction((Action) tag);
            }
        });
    }

    private void doQuery() {
        String reportId = etReportId.getText().toString().trim();
        if (TextUtils.isEmpty(reportId)) {
            Toast.makeText(this, "请输入报告ID", Toast.LENGTH_SHORT).show();
            return;
        }
        tvResult.setText("查询中...");
        btnAction.setVisibility(View.GONE);
        new Thread(() -> {
            try {
                int status = testRecordDao.getTestRecordStatus(reportId);
                TestRecord record = testRecordDao.getTestRecordById(reportId);
                runOnUiThread(() -> updateUIByStatus(status, record));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvResult.setText("查询失败：" + e.getMessage());
                    btnAction.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void updateUIByStatus(int status, @Nullable TestRecord r) {
        if (status == -1 || r == null) {
            tvResult.setText("未找到该报告ID的订单，请检查输入");
            btnAction.setVisibility(View.GONE);
            return;
        }
        switch (status) {
            case 1: // 已完成
                tvResult.setText("订单状态：已完成，报告已生成。\n测评：" + safe(r.title));
                btnAction.setText("查看报告");
                btnAction.setTag(Action.viewReport(r.reportId));
                btnAction.setVisibility(View.VISIBLE);
                break;
            case 2: // 待支付
                tvResult.setText("订单状态：待支付。\n测评：" + safe(r.title));
                btnAction.setText("去支付");
                btnAction.setTag(Action.viewReport(r.reportId));
                btnAction.setVisibility(View.VISIBLE);
                break;
            case 0: // 未完成，可继续答题
                tvResult.setText("订单状态：未完成，您可继续答题。\n测评：" + safe(r.title));
                btnAction.setText("继续答题");
                btnAction.setTag(Action.continueTest(r));
                btnAction.setVisibility(View.VISIBLE);
                break;
            default:
                tvResult.setText("状态未知，记录可能已变更");
                btnAction.setVisibility(View.GONE);
        }
    }

    private void handleAction(Action action) {
        if (action.type == ActionType.ViewReport) {
            Intent intent = new Intent(this, TestReportActivity.class);
            intent.putExtra("reportId", action.reportId);
            startActivity(intent);
        } else if (action.type == ActionType.ContinueTest && action.record != null) {
            TestRecord r = action.record;
            Intent intent = new Intent(this, ExercisesDetailActivity.class);
            intent.putExtra("reportId", r.reportId);
            intent.putExtra("currentIndex", r.currentIndex);
            intent.putExtra("answers", r.answers);
            intent.putExtra("id", getExerciseIdByTitle(r.title));
            intent.putExtra("title", r.title);
            startActivity(intent);
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    private enum ActionType { ViewReport, ContinueTest }
    private static class Action {
        final ActionType type;
        final String reportId;
        final TestRecord record;
        private Action(ActionType t, String rid, TestRecord r) { type = t; reportId = rid; record = r; }
        static Action viewReport(String rid) { return new Action(ActionType.ViewReport, rid, null); }
        static Action continueTest(TestRecord r) { return new Action(ActionType.ContinueTest, null, r); }
    }

    // 从标题映射到习题ID，需与题库保持一致
    private static int getExerciseIdByTitle(String title) {
        if (title == null) return 1;
        if (title.contains("恋爱心理成熟度")) return 1;
        if (title.contains("社交恐惧症量表")) return 2;
        if (title.contains("交友能力")) return 3;
        if (title.contains("汉密顿抑郁量表")) return 4;
        if (title.contains("焦虑程度")) return 5;
        if (title.contains("抑郁症程度")) return 6;
        if (title.contains("精神压力")) return 7;
        if (title.contains("抑郁应对方式")) return 8;
        if (title.contains("回避型依恋")) return 9;
        if (title.contains("人生质量")) return 10;
        return 1;
    }
}

