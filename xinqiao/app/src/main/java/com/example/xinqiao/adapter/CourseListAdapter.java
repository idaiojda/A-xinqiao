package com.example.xinqiao.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.VideoListActivity;
import com.example.xinqiao.bean.CourseBean;
import com.example.xinqiao.util.ui.ImageLoader;

public class CourseListAdapter extends ListAdapter<CourseBean, CourseListAdapter.ViewHolder> {
    private final Context mContext;
    private final int layoutResId;

    public CourseListAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.mContext = context;
        this.layoutResId = R.layout.course_item_purple;
    }

    public CourseListAdapter(Context context, int layoutResId) {
        super(DIFF_CALLBACK);
        this.mContext = context;
        this.layoutResId = layoutResId;
    }

    public static final DiffUtil.ItemCallback<CourseBean> DIFF_CALLBACK = new DiffUtil.ItemCallback<CourseBean>() {
        @Override
        public boolean areItemsTheSame(@NonNull CourseBean oldItem, @NonNull CourseBean newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull CourseBean oldItem, @NonNull CourseBean newItem) {
            return eq(oldItem.title, newItem.title)
                    && eq(oldItem.imgTitle, newItem.imgTitle)
                    && eq(oldItem.intro, newItem.intro);
        }

        private boolean eq(String a, String b) {
            return a == b || (a != null && a.equals(b));
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final CourseBean bean = getItem(position);
        if (bean == null) return;
        
        // 基础信息显示
        if (holder.tvImgTitle != null) holder.tvImgTitle.setText(bean.imgTitle);
        if (holder.tvTitle != null) holder.tvTitle.setText(bean.title);
        if (holder.tvDescription != null) holder.tvDescription.setText(bean.intro != null ? bean.intro : "了解心理健康的基本概念，学会识别和应对常见的心理问题");
        if (holder.ivCover != null) setImageById(bean.id, holder.ivCover);
        
        // 现代布局额外信息
        if (holder.tvChapterNumber != null) {
            holder.tvChapterNumber.setText("第" + bean.id + "章");
        }
        
        // 课程类别标签
        if (holder.tvCategoryTag != null) {
            String[] categories = {"心理健康", "情绪管理", "压力应对", "人际沟通", "自我认知", "心理调适"};
            holder.tvCategoryTag.setText(categories[(bean.id - 1) % categories.length]);
        }
        
        // 难度标签
        if (holder.tvDifficultyTag != null) {
            String[] difficulties = {"入门", "基础", "进阶", "高级"};
            holder.tvDifficultyTag.setText(difficulties[(bean.id - 1) % difficulties.length]);
        }
        
        // 学习人数（模拟数据）
        if (holder.tvStudentCount != null) {
            int students = 800 + bean.id * 150 + (int)(Math.random() * 200);
            if (students >= 1000) {
                holder.tvStudentCount.setText((students / 1000) + "k");
            } else {
                holder.tvStudentCount.setText(String.valueOf(students));
            }
        }
        
        // 课程时长（模拟数据）
        if (holder.tvDuration != null) {
            int minutes = 10 + bean.id * 3 + (int)(Math.random() * 5);
            holder.tvDuration.setText(minutes + "分钟");
        }
        
        // 评分（根据ID生成不同评分）
        if (holder.tvRating != null) {
            double rating = 4.5 + (bean.id % 5) * 0.1;
            holder.tvRating.setText(String.format("%.1f", rating));
        }
        
        // 旧版标签（兼容性）
        if (holder.tvTag != null) {
            if (bean.id <= 3) {
                holder.tvTag.setText("免费");
                holder.tvTag.setBackgroundResource(R.drawable.bg_tag_purple_free);
            } else {
                holder.tvTag.setText("会员");
                holder.tvTag.setBackgroundResource(R.drawable.bg_tag_purple_premium);
            }
        }
        
        // 课程数量（兼容性）
        if (holder.tvLessonCount != null) {
            holder.tvLessonCount.setText("12节课");
        }
        
        // 点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, VideoListActivity.class);
                intent.putExtra("id", bean.id);
                intent.putExtra("intro", bean.intro);
                mContext.startActivity(intent);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvImgTitle;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvTag;
        TextView tvLessonCount;
        TextView tvRating;
        TextView tvChapterNumber;
        TextView tvCategoryTag;
        TextView tvDifficultyTag;
        TextView tvStudentCount;
        TextView tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvImgTitle = itemView.findViewById(R.id.tv_img_title);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvTag = itemView.findViewById(R.id.tv_tag);
            tvLessonCount = itemView.findViewById(R.id.tv_lesson_count);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvChapterNumber = itemView.findViewById(R.id.tv_chapter_number);
            tvCategoryTag = itemView.findViewById(R.id.tv_category_tag);
            tvDifficultyTag = itemView.findViewById(R.id.tv_difficulty_tag);
            tvStudentCount = itemView.findViewById(R.id.tv_student_count);
            tvDuration = itemView.findViewById(R.id.tv_duration);
        }
    }

    private void setImageById(int id, ImageView iv) {
        final int resId = getImageResId(id);
        ImageLoader.loadImage(mContext, resId, iv);
    }

    private int getImageResId(int id) {
        switch (id) {
            case 1: return R.mipmap.chapter_1_icon;
            case 2: return R.mipmap.chapter_2_icon;
            case 3: return R.mipmap.chapter_3_icon;
            case 4: return R.mipmap.chapter_4_icon;
            case 5: return R.mipmap.chapter_5_icon;
            case 6: return R.mipmap.chapter_6_icon;
            case 7: return R.mipmap.chapter_7_icon;
            case 8: return R.mipmap.chapter_8_icon;
            case 9: return R.mipmap.chapter_9_icon;
            case 10: return R.mipmap.chapter_10_icon;
            default: return R.mipmap.chapter_1_icon;
        }
    }

    private int computeImageHeightPx(CourseBean bean) {
        // 三档高度：160dp/200dp/240dp，根据 id 做分配
        int mod = Math.abs(bean.id) % 3;
        int dp = 160 + mod * 40; // 160, 200, 240
        float density = mContext.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
