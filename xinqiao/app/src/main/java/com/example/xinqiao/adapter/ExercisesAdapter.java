package com.example.xinqiao.adapter;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.xinqiao.R;
import com.example.xinqiao.activity.ExercisesDetailActivity;
import com.example.xinqiao.bean.ExercisesBean;
import java.util.List;
import java.util.ArrayList;
import android.widget.Button;
import android.widget.EditText;
import java.util.Collections;
import android.text.Editable;
import android.text.TextWatcher;
import com.example.xinqiao.util.ImageLoader;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.app.Activity;
import com.example.xinqiao.utils.PaymentUtils;

public class ExercisesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EXERCISE = 1;
    private Context mContext;
    private List<ExercisesBean> ebl; // 当前显示的数据
    private List<ExercisesBean> allEbl; // 全部数据副本
    private List<RecommendAdapter.RecommendBean> recommendList;
    private boolean showHeader = true;
    private PaymentUtils paymentUtils;
    public ExercisesAdapter(Context context, boolean showHeader) {
        this.mContext = context;
        this.showHeader = showHeader;
        this.paymentUtils = new PaymentUtils(context);
    }
    // 兼容旧用法
    public ExercisesAdapter(Context context) {
        this(context, true);
    }
    public void setData(List<ExercisesBean> ebl) {
        this.ebl = ebl;
        notifyDataSetChanged();
    }
    // 新增：设置全量数据（不影响当前显示）
    public void setAllData(List<ExercisesBean> all) {
        this.allEbl = new ArrayList<>(all);
    }
    // 新增：搜索过滤方法
    public void filter(String keyword) {
        if (allEbl == null) return;
        if (keyword == null || keyword.isEmpty()) {
            ebl = new ArrayList<>();
        } else {
            String lowerKeyword = keyword.toLowerCase();
            List<ExercisesBean> filtered = new ArrayList<>();
            for (ExercisesBean bean : allEbl) {
                boolean match = false;
                if (bean.title != null && bean.title.toLowerCase().contains(lowerKeyword)) match = true;
                if (bean.content != null && bean.content.toLowerCase().contains(lowerKeyword)) match = true;
                if (bean.category != null && bean.category.toLowerCase().contains(lowerKeyword)) match = true;
                if (match) filtered.add(bean);
            }
            ebl = filtered;
        }
        notifyDataSetChanged();
    }
    public void setRecommendData(List<RecommendAdapter.RecommendBean> recommendList) {
        this.recommendList = recommendList;
        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        return (ebl == null ? 0 : ebl.size()) + (showHeader ? 1 : 0);
    }
    @Override
    public int getItemViewType(int position) {
        if (showHeader && position == 0) return TYPE_HEADER;
        return TYPE_EXERCISE;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_exercises_header, parent, false);
            return new HeaderViewHolder(view, mContext, recommendList, this);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.exercises_list_item, parent, false);
            return new ExerciseViewHolder(view);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            // HeaderViewHolder自管理推荐区块，无需重复绑定
        } else {
            int realPos = showHeader ? position - 1 : position;
            final ExercisesBean bean = ebl.get(realPos);
            ExerciseViewHolder vh = (ExerciseViewHolder) holder;
            vh.title.setText(bean.title);
            vh.subtitle.setText(bean.content);
            
            // 设置标签（免费/付费）
            if (bean.isPremium) {
                vh.tvTag.setText("付费");
                vh.tvTag.setTextColor(mContext.getResources().getColor(R.color.white));
                vh.tvTag.setBackgroundResource(R.drawable.bg_tag_premium);
                
                // 显示价格
                vh.tvPeople.setText(String.format("%.2f喜贝", bean.price));
            } else {
                vh.tvTag.setText("免费");
                vh.tvTag.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
                vh.tvTag.setBackgroundResource(R.drawable.bg_tag_free);
                
                // 显示人数
                vh.tvPeople.setText("1099.8万人测过");
            }
            
            // 设置右侧图片
            ImageView ivCover = holder.itemView.findViewById(R.id.iv_cover);
            if (ivCover != null) {
                // 使用统一的背景图片分配逻辑
                final int imageResId = bean.background != 0 ? bean.background : 
                    com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean.id);
                
                // 使用ImageLoader工具类加载图片
                ImageLoader.loadImage(mContext, imageResId, ivCover);
            }
            
            vh.itemView.setOnClickListener(v -> {
                if (bean == null) return;
                
                // 检查是否为付费习题且未购买
                if (bean.isPremium && !bean.isPurchased) {
                    // 检查登录状态
                    if (!readLoginStatus()) {
                        android.widget.Toast.makeText(mContext, "请先登录", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 显示购买对话框
                    showPurchaseDialog(bean);
                } else {
                    // 免费习题或已购买，直接进入
                    Intent intent = new Intent(mContext, ExercisesDetailActivity.class);
                    intent.putExtra("id", bean.id);
                    intent.putExtra("title", bean.title);
                    mContext.startActivity(intent);
                }
            });
        }
    }
    
    /**
     * 显示购买对话框
     */
    private void showPurchaseDialog(ExercisesBean bean) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("购买习题")
               .setMessage("您需要购买此习题才能查看内容\n习题价格：￥" + bean.price)
               .setPositiveButton("购买", (dialog, which) -> {
                   // 执行购买操作
                   purchaseExercise(bean);
               })
               .setNegativeButton("取消", null)
               .show();
    }
    
    /**
     * 购买习题
     */
    private void purchaseExercise(ExercisesBean bean) {
        String username = readLoginUserName();
        if (username == null || username.isEmpty()) {
            android.widget.Toast.makeText(mContext, "请先登录", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 调用支付工具类进行购买
        paymentUtils.getBalance(username, new PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                // 获取余额成功后，检查余额是否足够
                double balance = paymentUtils.getLastBalance();
                if (balance < bean.price) {
                    android.widget.Toast.makeText(mContext, "余额不足，请先充值", android.widget.Toast.LENGTH_SHORT).show();
                    // 跳转到充值页面
                    Intent intent = new Intent(mContext, com.example.xinqiao.activity.RechargeActivity.class);
                    mContext.startActivity(intent);
                } else {
                    // 执行购买逻辑
                    completeExercisePurchase(bean, username);
                }
            }
            
            @Override
            public void onError(String message) {
                android.widget.Toast.makeText(mContext, "获取余额失败：" + message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 完成习题购买
     */
    private void completeExercisePurchase(ExercisesBean bean, String username) {
        // 调用PaymentUtils进行实际购买操作
        paymentUtils.purchaseExercise(username, bean.id, bean.price, new PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                // 购买成功
                bean.isPurchased = true;
                android.widget.Toast.makeText(mContext, "购买成功", android.widget.Toast.LENGTH_SHORT).show();
                
                // 购买成功后跳转到习题详情
                Intent intent = new Intent(mContext, ExercisesDetailActivity.class);
                intent.putExtra("id", bean.id);
                intent.putExtra("title", bean.title);
                mContext.startActivity(intent);
                
                // 通知数据变化
                notifyDataSetChanged();
            }
            
            @Override
            public void onError(String message) {
                android.widget.Toast.makeText(mContext, "购买失败：" + message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 读取登录状态
     */
    private boolean readLoginStatus() {
        SharedPreferences sp = mContext.getSharedPreferences("loginInfo", Activity.MODE_PRIVATE);
        return sp.getBoolean("isLogin", false);
    }
    
    /**
     * 读取登录用户名
     */
    private String readLoginUserName() {
        SharedPreferences sp = mContext.getSharedPreferences("loginInfo", Activity.MODE_PRIVATE);
        return sp.getString("loginUserName", "");
    }
    // 头部ViewHolder（包含搜索框、banner、icon区、每日推荐、换一换等）
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvRecommend;
        TextView tvChange;
        RecommendAdapter recommendAdapter;
        List<RecommendAdapter.RecommendBean> currentShowList = new ArrayList<>();
        EditText etSearch;
        public HeaderViewHolder(View itemView, Context context, List<RecommendAdapter.RecommendBean> allRecommendList, ExercisesAdapter adapter) {
            super(itemView);
            rvRecommend = itemView.findViewById(R.id.rv_recommend);
            tvChange = itemView.findViewById(R.id.tv_change);
            etSearch = itemView.findViewById(R.id.et_search);
            etSearch.setFocusable(false);
            etSearch.setOnClickListener(v -> {
                Intent intent = new Intent(context, com.example.xinqiao.activity.ExercisesSearchActivity.class);
                context.startActivity(intent);
            });
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // 不做实时搜索，交由搜索页处理
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
            rvRecommend.setLayoutManager(new GridLayoutManager(context, 2));
            // 初始化推荐数据
            updateRecommend(allRecommendList);
            recommendAdapter = new RecommendAdapter(context, currentShowList, adapter.allEbl);
            rvRecommend.setAdapter(recommendAdapter);
            tvChange.setOnClickListener(v -> {
                updateRecommend(allRecommendList);
                recommendAdapter.setData(currentShowList);
            });
            // 分类icon点击事件
            LinearLayout llLove = itemView.findViewById(R.id.ll_love);
            LinearLayout llPerson = itemView.findViewById(R.id.ll_person);
            LinearLayout llCharacter = itemView.findViewById(R.id.ll_character);
            LinearLayout llHealth = itemView.findViewById(R.id.ll_health);
            LinearLayout llFamily = itemView.findViewById(R.id.ll_family);
            LinearLayout llAbility = itemView.findViewById(R.id.ll_ability);
            LinearLayout llProfession = itemView.findViewById(R.id.ll_profession);
            llLove.setOnClickListener(v -> jumpToCategory(context, "情感"));
            llPerson.setOnClickListener(v -> jumpToCategory(context, "人际"));
            llCharacter.setOnClickListener(v -> jumpToCategory(context, "性格"));
            llHealth.setOnClickListener(v -> jumpToCategory(context, "健康"));
            llFamily.setOnClickListener(v -> jumpToCategory(context, "亲子"));
            llAbility.setOnClickListener(v -> jumpToCategory(context, "能力"));
            llProfession.setOnClickListener(v -> jumpToCategory(context, "职业"));
        }
        private void updateRecommend(List<RecommendAdapter.RecommendBean> allList) {
            currentShowList.clear();
            if (allList != null && allList.size() > 4) {
                List<RecommendAdapter.RecommendBean> temp = new ArrayList<>(allList);
                Collections.shuffle(temp);
                currentShowList.addAll(temp.subList(0, 4));
            } else if (allList != null) {
                currentShowList.addAll(allList);
            }
        }
        private void jumpToCategory(Context context, String category) {
            Intent intent = new Intent(context, com.example.xinqiao.activity.CategoryListActivity.class);
            intent.putExtra("category", category);
            context.startActivity(intent);
        }
    }
    // 习题item ViewHolder
    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        public TextView title, subtitle, tvTag, tvPeople;
        public ExerciseViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
            tvTag = itemView.findViewById(R.id.tv_tag);
            tvPeople = itemView.findViewById(R.id.tv_people);
        }
    }
}