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
        this.layoutResId = R.layout.course_item;
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
        holder.tvImgTitle.setText(bean.imgTitle);
        holder.tvTitle.setText(bean.title);
        setImageById(bean.id, holder.ivCover);
        // 使用 ImageView 的 wrap_content + adjustViewBounds，让高度随图片自适应
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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvImgTitle = itemView.findViewById(R.id.tv_img_title);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }

    private void setImageById(int id, ImageView iv) {
        final int resId = getImageResId(id);
        ImageLoader.loadImageFitCenter(mContext, resId, iv);
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
