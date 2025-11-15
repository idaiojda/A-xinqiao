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
import com.example.xinqiao.adapter.ChatMessageAdapter;
import com.example.xinqiao.dao.ChatHistoryDao;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.util.ui.RecyclerViewOptimizer;
import com.example.xinqiao.util.ui.SpacesItemDecoration;
import android.widget.Toast;

import java.util.ArrayList;

public class ConsultationDetailActivity extends AppCompatActivity {
    private RecyclerView rvMessages;
    private ChatMessageAdapter adapter;
    private ChatHistoryDao chatHistoryDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation_detail);

        TextView tvTitle = findViewById(R.id.tv_title);
        ImageButton btnBack = findViewById(R.id.btn_back);
        rvMessages = findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(new ArrayList<>());
        rvMessages.setAdapter(adapter);

        adapter.setOnMessageActionListener(message -> {
            if (message == null) return;
            chatHistoryDao.deleteMessageByIdAsync(message.getId(), success -> {
                if (success) {
                    runOnUiThread(() -> adapter.removeById(message.getId()));
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
            // 过滤重复开场词：仅保留会话中的第一条 AI 问候
            java.util.List<com.example.xinqiao.bean.ChatHistory> filtered = new java.util.ArrayList<>();
            boolean greetingSeen = false;
            String greetPrefix = "你来啦，我是"; // 与 ChatViewModel 的开场词前缀保持一致
            if (history != null) {
                for (com.example.xinqiao.bean.ChatHistory ch : history) {
                    boolean isGreeting = ch != null && ch.getType() == 0 && ch.getContent() != null && ch.getContent().startsWith(greetPrefix);
                    if (isGreeting) {
                        if (!greetingSeen) {
                            greetingSeen = true;
                            filtered.add(ch);
                        }
                        // 已出现过则忽略
                    } else {
                        filtered.add(ch);
                    }
                }
            }
            runOnUiThread(() -> adapter.setData(filtered));
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
