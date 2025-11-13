package com.example.xinqiao.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.xinqiao.R;
import com.example.xinqiao.bean.ExercisesBean;
import com.example.xinqiao.bean.QuestionBean;
import com.example.xinqiao.util.ImageLoader;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import java.util.ArrayList;
import java.util.List;

public class CategoryListActivity extends AppCompatActivity {
    private com.example.xinqiao.utils.PaymentUtils paymentUtils;
    private RecyclerView rvList;
    private TextView tvTitle, tvEmpty;
    private CategoryListAdapter adapter;
    private List<ExercisesBean> allList = new ArrayList<>();
    private List<ExercisesBean> showList = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_list);
        rvList = findViewById(R.id.rv_category_list);
        tvTitle = findViewById(R.id.tv_category_title);
        tvEmpty = findViewById(R.id.tv_empty);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish()); // 只保留finish()
        
        // 初始化支付工具类
        paymentUtils = new com.example.xinqiao.utils.PaymentUtils(this);
        
        String category = getIntent().getStringExtra("category");
        tvTitle.setText(category);
        // 加载全部测评数据
        allList = getAllExercises();
        // 过滤
        showList.clear();
        for (ExercisesBean bean : allList) {
            if (bean.category != null && bean.category.equals(category)) {
                showList.add(bean);
            }
        }
        adapter = new CategoryListAdapter(showList);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.setAdapter(adapter);
        
        // 优化RecyclerView性能
        RecyclerViewOptimizer.optimizeDefault(rvList);
        
        tvEmpty.setVisibility(showList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        // 释放RecyclerView资源，防止内存泄漏
        if (rvList != null) {
            rvList.setAdapter(null);
        }
        super.onDestroy();
    }
    
    /**
     * 读取登录状态
     */
    private boolean readLoginStatus() {
        SharedPreferences sp = getSharedPreferences("loginInfo", Activity.MODE_PRIVATE);
        return sp.getBoolean("isLogin", false);
    }
    
    /**
     * 读取登录用户名
     */
    private String readLoginUserName() {
        SharedPreferences sp = getSharedPreferences("loginInfo", Activity.MODE_PRIVATE);
        return sp.getString("loginUserName", "");
    }
    
    /**
     * 显示购买对话框
     */
    private void showPurchaseDialog(ExercisesBean bean) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            android.widget.Toast.makeText(this, "请先登录", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 调用支付工具类进行购买
        paymentUtils.getBalance(username, new com.example.xinqiao.utils.PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                // 获取余额成功后，检查余额是否足够
                double balance = paymentUtils.getLastBalance();
                if (balance < bean.price) {
                    android.widget.Toast.makeText(CategoryListActivity.this, "余额不足，请先充值", android.widget.Toast.LENGTH_SHORT).show();
                    // 跳转到充值页面
                    Intent intent = new Intent(CategoryListActivity.this, com.example.xinqiao.activity.RechargeActivity.class);
                    startActivity(intent);
                } else {
                    // 执行购买逻辑
                    completeExercisePurchase(bean, username);
                }
            }
            
            @Override
            public void onError(String message) {
                android.widget.Toast.makeText(CategoryListActivity.this, "获取余额失败：" + message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 完成习题购买
     */
    private void completeExercisePurchase(ExercisesBean bean, String username) {
        // 调用PaymentUtils进行实际购买操作
        paymentUtils.purchaseExercise(username, bean.id, bean.price, new com.example.xinqiao.utils.PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                // 购买成功
                bean.isPurchased = true;
                android.widget.Toast.makeText(CategoryListActivity.this, "购买成功", android.widget.Toast.LENGTH_SHORT).show();
                
                // 购买成功后跳转到习题详情
                Intent intent = new Intent(CategoryListActivity.this, ExercisesDetailActivity.class);
                intent.putExtra("id", bean.id);
                intent.putExtra("title", bean.title);
                startActivity(intent);
                
                // 通知数据变化
                adapter.notifyDataSetChanged();
            }
            
            @Override
            public void onError(String message) {
                android.widget.Toast.makeText(CategoryListActivity.this, "购买失败：" + message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    // 模拟获取全部测评数据（可与ExercisesView保持一致）
    private List<ExercisesBean> getAllExercises() {
        List<ExercisesBean> ebl = new ArrayList<>();
        // 1. 测测你的恋爱心理成熟度
        ExercisesBean bean1 = new ExercisesBean();
        bean1.id = 1;
        bean1.title = "测测你的恋爱心理成熟度";
        bean1.content = "共计6题";
        bean1.background = R.drawable.bg_1;
        bean1.category = "情感";
        bean1.isPremium = false; // 免费
        List<QuestionBean> q1 = new ArrayList<>();
        q1.add(new QuestionBean("你在恋爱中更看重什么？", new String[]{"安全感", "激情", "陪伴", "成长"}, 0));
        q1.add(new QuestionBean("遇到分歧时你通常会？", new String[]{"主动沟通", "冷处理", "顺其自然", "寻求朋友帮助"}, 0));
        q1.add(new QuestionBean("你对恋人最大的期待是？", new String[]{"理解和包容", "浪漫惊喜", "共同进步", "经济支持"}, 0));
        q1.add(new QuestionBean("你是否容易因小事和恋人争吵？", new String[]{"很少", "偶尔", "经常", "总是"}, 0));
        q1.add(new QuestionBean("你会主动表达自己的情感吗？", new String[]{"经常", "偶尔", "很少", "几乎不"}, 0));
        q1.add(new QuestionBean("你认为恋爱中最重要的品质是？", new String[]{"信任", "忠诚", "独立", "包容"}, 0));
        bean1.questions = q1;
        ebl.add(bean1);
        // 2. 社交恐惧症量表(SPIN)
        ExercisesBean bean2 = new ExercisesBean();
        bean2.id = 2;
        bean2.title = "社交恐惧症量表(SPIN)";
        bean2.content = "共计5题";
        bean2.background = R.drawable.bg_2;
        bean2.category = "人际";
        bean2.isPremium = false; // 免费
        List<QuestionBean> q2 = new ArrayList<>();
        q2.add(new QuestionBean("你在陌生人面前说话会感到紧张吗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q2.add(new QuestionBean("你是否害怕在公共场合被注视？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q2.add(new QuestionBean("你会因为害怕尴尬而避免社交活动吗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q2.add(new QuestionBean("你在小组讨论时会主动发言吗？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q2.add(new QuestionBean("你是否担心自己的表现会被别人评价？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        bean2.questions = q2;
        ebl.add(bean2);
        // 3. 测测你的交友能力
        ExercisesBean bean3 = new ExercisesBean();
        bean3.id = 3;
        bean3.title = "测测你的交友能力";
        bean3.content = "共计4题";
        bean3.background = R.drawable.bg_3;
        bean3.category = "人际";
        bean3.isPremium = true; // 付费
        bean3.price = 9.9;
        List<QuestionBean> q3 = new ArrayList<>();
        q3.add(new QuestionBean("你是否容易和陌生人搭话？", new String[]{"很容易", "一般", "有点难", "很难"}, 0));
        q3.add(new QuestionBean("你有几个可以倾诉的朋友？", new String[]{"很多", "几个", "很少", "没有"}, 0));
        q3.add(new QuestionBean("你会主动组织聚会或活动吗？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q3.add(new QuestionBean("遇到矛盾时你会如何处理？", new String[]{"主动沟通", "回避", "冷战", "求助他人"}, 0));
        bean3.questions = q3;
        ebl.add(bean3);
        // 4. 汉密顿抑郁量表–HRSD
        ExercisesBean bean4 = new ExercisesBean();
        bean4.id = 4;
        bean4.title = "汉密顿抑郁量表–HRSD";
        bean4.content = "共计6题";
        bean4.background = R.drawable.bg_4;
        bean4.category = "健康";
        bean4.isPremium = true; // 付费
        bean4.price = 12.8;
        List<QuestionBean> q4 = new ArrayList<>();
        q4.add(new QuestionBean("你最近是否经常感到情绪低落？", new String[]{"没有", "轻度", "中度", "重度"}, 0));
        q4.add(new QuestionBean("你是否对平时感兴趣的事物失去兴趣？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        q4.add(new QuestionBean("你是否经常感到疲惫或没有精力？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        q4.add(new QuestionBean("你的睡眠质量如何？", new String[]{"很好", "一般", "较差", "很差"}, 0));
        q4.add(new QuestionBean("你是否有自责或无价值感？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        q4.add(new QuestionBean("你是否有自杀念头？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        bean4.questions = q4;
        ebl.add(bean4);
        // 5. 焦虑程度测试
        ExercisesBean bean5 = new ExercisesBean();
        bean5.id = 5;
        bean5.title = "焦虑程度测试";
        bean5.content = "共计5题";
        bean5.background = R.drawable.bg_1;
        bean5.category = "健康";
        bean5.isPremium = true; // 付费
        bean5.price = 15.0;
        List<QuestionBean> q5 = new ArrayList<>();
        q5.add(new QuestionBean("你是否经常感到紧张或坐立不安？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new QuestionBean("你是否容易为小事担忧？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new QuestionBean("你是否经常感到心慌或出汗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new QuestionBean("你是否会因为担心而影响睡眠？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new QuestionBean("你是否觉得难以控制自己的焦虑情绪？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        bean5.questions = q5;
        ebl.add(bean5);
        // 6. 抑郁症程度测试
        ExercisesBean bean6 = new ExercisesBean();
        bean6.id = 6;
        bean6.title = "抑郁症程度测试";
        bean6.content = "共计5题";
        bean6.background = R.drawable.bg_6;
        bean6.category = "健康";
        bean6.isPremium = false; // 免费
        List<QuestionBean> q6 = new ArrayList<>();
        q6.add(new QuestionBean("你是否经常感到情绪低落？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new QuestionBean("你是否对生活失去兴趣？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new QuestionBean("你是否经常感到疲惫或无力？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new QuestionBean("你是否经常自责或觉得自己没用？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new QuestionBean("你是否有过自杀的念头？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        bean6.questions = q6;
        ebl.add(bean6);
        // 7. 精神压力程度测试
        ExercisesBean bean7 = new ExercisesBean();
        bean7.id = 7;
        bean7.title = "精神压力程度测试";
        bean7.content = "共计4题";
        bean7.background = R.drawable.bg_7;
        bean7.category = "健康";
        bean7.isPremium = true; // 付费
        bean7.price = 18.5;
        List<QuestionBean> q7 = new ArrayList<>();
        q7.add(new QuestionBean("你是否经常感到压力大？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q7.add(new QuestionBean("你是否因压力影响睡眠？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q7.add(new QuestionBean("你是否因压力而情绪波动？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q7.add(new QuestionBean("你是否有缓解压力的有效方法？", new String[]{"有", "偶尔有", "很少有", "没有"}, 0));
        bean7.questions = q7;
        ebl.add(bean7);
        // 8. 抑郁应对方式评估
        ExercisesBean bean8 = new ExercisesBean();
        bean8.id = 8;
        bean8.title = "抑郁应对方式评估";
        bean8.content = "共计5题";
        bean8.background = R.drawable.bg_8;
        bean8.category = "健康";
        bean8.isPremium = true; // 付费
        bean8.price = 20.0;
        List<QuestionBean> q8 = new ArrayList<>();
        q8.add(new QuestionBean("你遇到挫折时会？", new String[]{"自我安慰", "寻求帮助", "消极回避", "积极面对"}, 0));
        q8.add(new QuestionBean("你倾向于如何调节情绪？", new String[]{"运动", "倾诉", "独处", "娱乐"}, 0));
        q8.add(new QuestionBean("你是否会主动寻求心理咨询？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q8.add(new QuestionBean("你是否会写日记或记录情绪？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q8.add(new QuestionBean("你是否愿意与亲友分享自己的困扰？", new String[]{"非常愿意", "偶尔", "很少", "不愿意"}, 0));
        bean8.questions = q8;
        ebl.add(bean8);
        // 9. 回避型依恋测试
        ExercisesBean bean9 = new ExercisesBean();
        bean9.id = 9;
        bean9.title = "回避型依恋测试";
        bean9.content = "共计4题";
        bean9.background = R.drawable.bg_9;
        bean9.category = "性格";
        bean9.isPremium = false; // 免费
        List<QuestionBean> q9 = new ArrayList<>();
        q9.add(new QuestionBean("你是否害怕与人建立亲密关系？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q9.add(new QuestionBean("你是否习惯独处？", new String[]{"非常习惯", "偶尔", "不太习惯", "不习惯"}, 0));
        q9.add(new QuestionBean("你是否会主动疏远亲近的人？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q9.add(new QuestionBean("你是否觉得依赖别人会让你不安？", new String[]{"非常不安", "有点不安", "无所谓", "不会"}, 0));
        bean9.questions = q9;
        ebl.add(bean9);
        // 10. 人生质量评估
        ExercisesBean bean10 = new ExercisesBean();
        bean10.id = 10;
        bean10.title = "人生质量评估";
        bean10.content = "共计5题";
        bean10.background = R.drawable.bg_10;
        bean10.category = "能力";
        bean10.isPremium = true; // 付费
        bean10.price = 25.0;
        List<QuestionBean> q10 = new ArrayList<>();
        q10.add(new QuestionBean("你对目前的生活满意吗？", new String[]{"非常满意", "比较满意", "一般", "不满意"}, 0));
        q10.add(new QuestionBean("你是否有明确的人生目标？", new String[]{"非常明确", "比较明确", "不太明确", "没有"}, 0));
        q10.add(new QuestionBean("你是否经常感到快乐？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q10.add(new QuestionBean("你是否有良好的人际关系？", new String[]{"非常好", "一般", "较差", "很差"}, 0));
        q10.add(new QuestionBean("你是否有健康的生活方式？", new String[]{"非常健康", "比较健康", "一般", "不健康"}, 0));
        bean10.questions = q10;
        ebl.add(bean10);
        return ebl;
    }
    // 内部适配器
    public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.ViewHolder> {
        private List<ExercisesBean> data;
        public CategoryListAdapter(List<ExercisesBean> data) { this.data = data; }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_category_list, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ExercisesBean bean = data.get(position);
            holder.tvTitle.setText(bean.title);
            holder.tvDesc.setText(bean.content);
            
            // 设置标签（免费/付费）
            if (bean.isPremium) {
                holder.tvTag.setText("付费");
                holder.tvTag.setTextColor(getResources().getColor(R.color.white));
                holder.tvTag.setBackgroundResource(R.drawable.bg_tag_premium);
            } else {
                holder.tvTag.setText("免费");
                holder.tvTag.setTextColor(getResources().getColor(R.color.colorPrimary));
                holder.tvTag.setBackgroundResource(R.drawable.bg_tag_free);
            }
            
            // 使用统一的背景图片分配逻辑
            final int imageResId = bean.background != 0 ? bean.background : 
                com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean.id);
            
            // 使用ImageLoader工具类加载图片
            ImageLoader.loadImage(CategoryListActivity.this, imageResId, holder.ivCover);
            
            holder.itemView.setOnClickListener(v -> {
                // 检查是否为付费习题且未购买
                if (bean.isPremium && !bean.isPurchased) {
                    // 检查登录状态
                    if (!readLoginStatus()) {
                        android.widget.Toast.makeText(CategoryListActivity.this, "请先登录", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 显示购买对话框
                    showPurchaseDialog(bean);
                } else {
                    // 免费习题或已购买，直接进入
                    Intent intent = new Intent(CategoryListActivity.this, ExercisesDetailActivity.class);
                    intent.putExtra("id", bean.id);
                    intent.putExtra("title", bean.title);
                    startActivity(intent);
                }
            });
        }
        @Override
        public int getItemCount() { return data == null ? 0 : data.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvTag;
            ImageView ivCover;
            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_category_title_item);
                tvDesc = itemView.findViewById(R.id.tv_category_desc);
                tvTag = itemView.findViewById(R.id.tv_category_tag);
                ivCover = itemView.findViewById(R.id.iv_category_cover);
            }
        }
    }
}