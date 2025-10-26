package com.example.xinqiao.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.ArticleBean;
import com.example.xinqiao.activity.ArticleDetailActivity;
import com.example.xinqiao.util.ImageLoader;

import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {
    // 上下文对象，用于启动Activity等
    private Context mContext;
    // 文章数据列表
    private List<ArticleBean> mArticleList;
    // item点击事件监听器
    private OnItemClickListener mListener;
    // 是否显示阅读进度
    private boolean mShowReadProgress;

    /**
     * 构造方法，初始化适配器
     * @param context 上下文
     * @param articleList 文章数据列表
     * @param showReadProgress 是否显示阅读进度
     */
    public ArticleAdapter(Context context, List<ArticleBean> articleList, boolean showReadProgress) {
        this.mContext = context;
        this.mArticleList = articleList;
        this.mShowReadProgress = showReadProgress;
    }

    /**
     * 创建ViewHolder，加载每个item的布局
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_article, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 绑定数据到ViewHolder，设置标题、分类、摘要、图片、阅读时间、进度等
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArticleBean article = mArticleList.get(position);
        holder.tvTitle.setText(article.title);
        holder.tvCategory.setText(article.category);

        // 摘要显示与隐藏
        if (article.summary != null && !article.summary.isEmpty()) {
            holder.tvSummary.setVisibility(View.VISIBLE);
            holder.tvSummary.setText(article.summary);
        } else {
            holder.tvSummary.setVisibility(View.GONE);
        }
        // 图片显示与隐藏
        if (article.imageResId != 0) {
            holder.ivImage.setVisibility(View.VISIBLE);
            
            // 使用ImageLoader工具类加载图片
            ImageLoader.loadImage(mContext, article.imageResId, holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        // 显示相对阅读时间
        String timeAgo = DateUtils.getRelativeTimeSpanString(
                article.readTimestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        ).toString();
        holder.tvReadTime.setText(timeAgo);

        // 根据是否显示阅读进度来设置进度条的可见性
        if (mShowReadProgress && article.readProgress > 0) {
            holder.tvReadProgress.setVisibility(View.VISIBLE);
            holder.tvReadProgress.setText(String.format("已读%d%%", article.readProgress));
        } else {
            holder.tvReadProgress.setVisibility(View.GONE);
        }

        // 设置点击事件，跳转到文章详情页
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(article);
            }
            // 跳转到详情页
            Intent intent = new Intent(mContext, ArticleDetailActivity.class);
            intent.putExtra("title", article.title);
            intent.putExtra("category", article.category);
            intent.putExtra("summary", article.summary);
            intent.putExtra("content", article.content);
            intent.putExtra("imageResId", article.imageResId);
            intent.putExtra("articleId", article.articleId);
            mContext.startActivity(intent);
        });
    }

    /**
     * 返回文章总数
     */
    @Override
    public int getItemCount() {
        return mArticleList == null ? 0 : mArticleList.size();
    }

    /**
     * 设置新的数据并刷新列表
     */
    public void setData(List<ArticleBean> articleList) {
        this.mArticleList = articleList;
        notifyDataSetChanged();
    }

    /**
     * 设置item点击事件监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    /**
     * ViewHolder 内部类，缓存item视图，提升性能
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;        // 文章标题
        TextView tvCategory;     // 文章分类
        TextView tvReadProgress; // 阅读进度
        TextView tvReadTime;     // 阅读时间
        TextView tvSummary;      // 文章摘要
        ImageView ivImage;       // 文章图片

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_article_title);
            tvCategory = itemView.findViewById(R.id.tv_article_category);
            tvReadProgress = itemView.findViewById(R.id.tv_read_progress);
            tvReadTime = itemView.findViewById(R.id.tv_read_time);
            tvSummary = itemView.findViewById(R.id.tv_article_summary);
            ivImage = itemView.findViewById(R.id.iv_article_image);
        }
    }

    /**
     * item点击事件接口
     */
    public interface OnItemClickListener {
        void onItemClick(ArticleBean article);
    }
}