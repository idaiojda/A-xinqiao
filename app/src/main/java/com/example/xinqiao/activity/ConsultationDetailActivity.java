package com.example.xinqiao.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.ChatMessageAdapter;
import com.example.xinqiao.bean.ChatHistory;
import com.example.xinqiao.dao.ChatHistoryDao;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import com.example.xinqiao.util.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.List;

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

        // 列表间距与性能优化
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
            runOnUiThread(() -> adapter.setData(history != null ? history : new ArrayList<>()));
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
