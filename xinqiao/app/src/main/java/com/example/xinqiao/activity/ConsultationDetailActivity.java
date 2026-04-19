package com.example.xinqiao.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.ChatHistoryAdapter;
import com.example.xinqiao.dao.ChatHistoryDao;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.util.ui.RecyclerViewOptimizer;
import com.example.xinqiao.util.ui.SpacesItemDecoration;
import android.widget.Toast;

import java.util.ArrayList;

public class ConsultationDetailActivity extends AppCompatActivity {
    private RecyclerView rvMessages;
    private ChatHistoryAdapter adapter;
    private ChatHistoryDao chatHistoryDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_detail);

        TextView tvTitle = findViewById(R.id.tv_title);
        ImageButton btnBack = findViewById(R.id.btn_back);
        rvMessages = findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatHistoryAdapter(new ArrayList<>());
        rvMessages.setAdapter(adapter);

        adapter.setOnMessageActionListener(message -> {
            if (message == null) return;
            chatHistoryDao.deleteMessageByIdAsync((int)message.getId(), success -> {
                if (success) {
                    runOnUiThread(() -> adapter.removeById((int)message.getId()));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "删除失败，请稍后重试", Toast.LENGTH_SHORT).show());
                }
            });
        });

        // 美化间距与滚动体验
        int spacingPx = dpToPx(8);
        rvMessages.addItemDecoration(new SpacesItemDecoration(spacingPx, true));
        rvMessages.setClipToPadding(false);
        rvMessages.setPadding(spacingPx, spacingPx, spacingPx, spacingPx);
        RecyclerViewOptimizer.optimizeDefault(rvMessages);

        btnBack.setOnClickListener(v -> finish());

        Intent it = getIntent();
        String title = it.getStringExtra("title");
        int sessionId = it.getIntExtra("sessionId", 0);
        if (tvTitle != null) tvTitle.setText(title != null ? title : "咨询详情");

        chatHistoryDao = new ChatHistoryDao(this);
        String userName = AnalysisUtils.readLoginUserName(this);
        chatHistoryDao.getChatHistoryAsync(userName != null ? userName : "", sessionId, history -> {
            // 数据库已清理，直接使用历史数据
            runOnUiThread(() -> adapter.setData(history != null ? history : new java.util.ArrayList<>()));
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
