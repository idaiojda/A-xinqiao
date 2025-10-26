package com.example.xinqiao.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.ExercisesDetailActivity;
import com.example.xinqiao.bean.ExercisesBean;
import com.example.xinqiao.util.ImageLoader;

import java.util.List;

public class RecommendAdapter extends RecyclerView.Adapter<RecommendAdapter.ViewHolder> {
    private Context mContext;
    private List<RecommendBean> dataList;
    private List<ExercisesBean> exercisesList;

    public RecommendAdapter(Context context, List<RecommendBean> dataList, List<ExercisesBean> exercisesList) {
        this.mContext = context;
        this.dataList = dataList;
        this.exercisesList = exercisesList;
    }

    public void setData(List<RecommendBean> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_recommend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendBean bean = dataList.get(position);
        holder.tvTitle.setText(bean.title);
        holder.tvPeople.setText(bean.peopleText);
        
        // 使用ImageLoader工具类加载图片
        final int imageResId = bean.imageResId;
        ImageLoader.loadImage(mContext, imageResId, holder.ivCover);
        
        holder.tvTag.setText("免费");
        holder.itemView.setOnClickListener(v -> {
            // 通过id查找对应的ExercisesBean
            ExercisesBean match = null;
            if (exercisesList != null) {
                for (ExercisesBean ex : exercisesList) {
                    if (ex.id == bean.id) {
                        match = ex;
                        break;
                    }
                }
            }
            Intent intent = new Intent(mContext, ExercisesDetailActivity.class);
            intent.putExtra("id", bean.id);
            intent.putExtra("title", bean.title);
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvTag, tvPeople;
        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTag = itemView.findViewById(R.id.tv_tag);
            tvPeople = itemView.findViewById(R.id.tv_people);
        }
    }

    // 推荐数据Bean
    public static class RecommendBean {
        public int id;
        public String title;
        public String peopleText;
        public int imageResId;
        public RecommendBean(int id, String title, String peopleText, int imageResId) {
            this.id = id;
            this.title = title;
            this.peopleText = peopleText;
            this.imageResId = imageResId;
        }
    }
}