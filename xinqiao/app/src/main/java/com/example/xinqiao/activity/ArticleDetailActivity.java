package com.example.xinqiao.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;

public class ArticleDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        TextView tvTitle = findViewById(R.id.tv_detail_title);
        ImageView ivImage = findViewById(R.id.iv_detail_image);
        TextView tvCategory = findViewById(R.id.tv_detail_category);
        TextView tvSummary = findViewById(R.id.tv_detail_summary);
        TextView tvContent = findViewById(R.id.tv_detail_content);

        Intent intent = getIntent();
        tvTitle.setText(intent.getStringExtra("title"));
        tvCategory.setText(intent.getStringExtra("category"));
        String summary = intent.getStringExtra("summary");
        if (summary != null && !summary.isEmpty()) {
            tvSummary.setText(summary);
            tvSummary.setVisibility(TextView.VISIBLE);
        } else {
            tvSummary.setVisibility(TextView.GONE);
        }
        String content = intent.getStringExtra("content");
        tvContent.setText(content);
        int imageResId = intent.getIntExtra("imageResId", 0);
        if (imageResId != 0) {
            ivImage.setImageResource(imageResId);
            ivImage.setVisibility(ImageView.VISIBLE);
        } else {
            ivImage.setVisibility(ImageView.GONE);
        }

        ImageButton btnBack = findViewById(R.id.btn_detail_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        ImageButton btnShare = findViewById(R.id.btn_detail_share);
        ImageButton btnFavorite = findViewById(R.id.btn_detail_favorite);
        final boolean[] isFavorited = {false};
        
        // 获取文章信息
        String userName = com.example.xinqiao.utils.AnalysisUtils.readLoginUserName(this);
        int articleId = intent.getIntExtra("articleId", 0);
        
        // 检查文章是否已收藏
        if (userName != null && !userName.isEmpty() && articleId != 0) {
            com.example.xinqiao.dao.ArticleFavoriteDao favoriteDao = new com.example.xinqiao.dao.ArticleFavoriteDao(this);
            favoriteDao.isFavoriteExistsAsync(userName, articleId, exists -> {
                isFavorited[0] = exists;
                btnFavorite.setImageResource(exists ? R.drawable.ic_favorite_selected : R.drawable.ic_favorite);
            });
        }
        
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shareText = tvTitle.getText().toString() + "\n" + tvSummary.getText().toString() + "\n" + tvContent.getText().toString();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "分享文章"));
            }
        });
        
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userName == null || userName.isEmpty() || articleId == 0) {
                    Toast.makeText(ArticleDetailActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                com.example.xinqiao.dao.ArticleFavoriteDao favoriteDao = new com.example.xinqiao.dao.ArticleFavoriteDao(ArticleDetailActivity.this);
                
                if (isFavorited[0]) {
                    // 取消收藏
                    favoriteDao.deleteFavoriteAsync(userName, articleId, success -> {
                        if (success) {
                            isFavorited[0] = false;
                            btnFavorite.setImageResource(R.drawable.ic_favorite);
                            Toast.makeText(ArticleDetailActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // 添加收藏
                    com.example.xinqiao.bean.ArticleBean article = new com.example.xinqiao.bean.ArticleBean();
                    article.userName = userName;
                    article.articleId = articleId;
                    article.title = intent.getStringExtra("title");
                    article.content = intent.getStringExtra("content");
                    article.category = intent.getStringExtra("category");
                    article.summary = intent.getStringExtra("summary");
                    article.imageResId = intent.getIntExtra("imageResId", 0);
                    
                    favoriteDao.saveFavoriteAsync(article, success -> {
                        if (success) {
                            isFavorited[0] = true;
                            btnFavorite.setImageResource(R.drawable.ic_favorite_selected);
                            Toast.makeText(ArticleDetailActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        // 只保存一次阅读历史
        if (userName != null && !userName.isEmpty() && articleId != 0) {
            com.example.xinqiao.dao.ArticleHistoryDao dao = new com.example.xinqiao.dao.ArticleHistoryDao(this);
            dao.isArticleHistoryExistsAsync(userName, articleId, exists -> {
                if (!exists) {
                    com.example.xinqiao.bean.ArticleBean article = new com.example.xinqiao.bean.ArticleBean();
                    article.userName = userName;
                    article.articleId = articleId;
                    article.title = intent.getStringExtra("title");
                    article.content = intent.getStringExtra("content");
                    article.category = intent.getStringExtra("category");
                    article.readTimestamp = System.currentTimeMillis();
                    article.readProgress = 100;
                    article.summary = intent.getStringExtra("summary");
                    article.imageResId = intent.getIntExtra("imageResId", 0);
                    dao.saveArticleHistoryAsync(article, success -> {
                        // 可选：保存成功/失败提示
                    });
                }
            });
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}