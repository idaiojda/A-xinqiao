package com.example.xinqiao.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.ExercisesDetailActivity;
import com.example.xinqiao.bean.ExercisesBean;

import java.util.ArrayList;
import java.util.List;

public class HotRankAdapter extends RecyclerView.Adapter<HotRankAdapter.ViewHolder> {
    private Context mContext;
    private List<HotItem> dataList = new ArrayList<>();
    private List<ExercisesBean> exercisesList;
    private OnItemClickListener onItemClickListener;

    public HotRankAdapter(Context context, List<HotItem> dataList, List<ExercisesBean> exercisesList) {
        this.mContext = context;
        this.dataList = dataList;
        this.exercisesList = exercisesList;
    }

    public void setData(List<HotItem> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(HotItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_hot_rank, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HotItem item = dataList.get(position);
        int rank = position + 1;
        holder.tvRank.setText(String.valueOf(rank));
        holder.tvTitle.setText(item.title);
        holder.tvPeople.setText(formatPeopleText(item.count));

        // 高亮前3名
        if (rank <= 3) {
            holder.tvRank.setTextColor(0xFFFF8C4B); // 橙色
            holder.tvTitle.setTextColor(0xFF222222);
        } else {
            holder.tvRank.setTextColor(0xFF999999);
            holder.tvTitle.setTextColor(0xFF333333);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            } else {
                Intent intent = new Intent(mContext, ExercisesDetailActivity.class);
                intent.putExtra("id", item.id);
                intent.putExtra("title", item.title);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvTitle, tvPeople;
        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvPeople = itemView.findViewById(R.id.tv_people);
        }
    }

    public static class HotItem {
        public int id;
        public String title;
        public int count; // 人数（单位：人）
        public HotItem(int id, String title, int count) {
            this.id = id;
            this.title = title;
            this.count = count;
        }
    }

    private String formatPeopleText(int count) {
        if (count >= 10000) {
            double w = count / 10000.0;
            return String.format("%.1f万人测过", w);
        } else {
            return count + "人测过";
        }
    }
}

