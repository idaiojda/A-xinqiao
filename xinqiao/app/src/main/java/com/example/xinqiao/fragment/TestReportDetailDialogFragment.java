package com.example.xinqiao.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.TestReportItem;

public class TestReportDetailDialogFragment extends DialogFragment {

    public static TestReportDetailDialogFragment newInstance(TestReportItem item) {
        TestReportDetailDialogFragment f = new TestReportDetailDialogFragment();
        Bundle args = new Bundle();
        args.putLong("id", item.id);
        args.putString("reportId", item.reportId);
        args.putString("type", item.type);
        args.putInt("score", Math.round(item.score));
        args.putString("risk", item.riskLevel);
        args.putString("date", item.date);
        args.putString("details", item.details);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_report_detail, container, false);
        TextView tvType = view.findViewById(R.id.tv_type);
        TextView tvDate = view.findViewById(R.id.tv_date);
        TextView tvScore = view.findViewById(R.id.tv_score);
        TextView tvRisk = view.findViewById(R.id.tv_risk);
        TextView tvDetails = view.findViewById(R.id.tv_details);
        Button btnShare = view.findViewById(R.id.btn_share);
        Button btnClose = view.findViewById(R.id.btn_close);

        Bundle args = getArguments();
        if (args != null) {
            tvType.setText(args.getString("type", ""));
            tvDate.setText(args.getString("date", ""));
            tvScore.setText("分数：" + args.getInt("score", 0));
            tvRisk.setText("风险：" + args.getString("risk", ""));
            tvDetails.setText(args.getString("details", ""));
        }

        btnShare.setOnClickListener(v -> {
            String shareText = buildShareText();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(intent, "分享报告"));
        });

        btnClose.setOnClickListener(v -> dismiss());
        return view;
    }

    private String buildShareText() {
        Bundle args = getArguments();
        String type = args != null ? args.getString("type", "") : "";
        String date = args != null ? args.getString("date", "") : "";
        int score = args != null ? args.getInt("score", 0) : 0;
        String risk = args != null ? args.getString("risk", "") : "";
        String details = args != null ? args.getString("details", "") : "";
        return "测评报告\n" +
                "类型：" + type + "\n" +
                "日期：" + date + "\n" +
                "分数：" + score + "\n" +
                "风险：" + risk + "\n\n" +
                "详情：\n" + details;
    }
}
