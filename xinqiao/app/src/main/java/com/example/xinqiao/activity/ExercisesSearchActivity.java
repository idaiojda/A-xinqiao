package com.example.xinqiao.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.xinqiao.R;
import com.example.xinqiao.adapter.ExercisesAdapter;
import com.example.xinqiao.bean.ExercisesBean;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import com.google.android.flexbox.FlexboxLayout;
import com.example.xinqiao.utils.AnalysisUtils;

public class ExercisesSearchActivity extends Activity {
    private EditText etSearch;
    private ImageView ivBack, ivClear;
    private RecyclerView rvResult;
    private TextView tvEmpty;
    private ExercisesAdapter adapter;
    private List<ExercisesBean> allList = new ArrayList<>();
    private FlexboxLayout flowHot, flowHistory;
    private TextView tvClearHistory;
    private SharedPreferences sp;
    private static final String SP_HISTORY = "search_history";
    private static final int MAX_HISTORY = 10;
    private String[] hotWords = {"焦虑", "抑郁", "社交", "压力", "自信", "亲子", "能力"};
    private String userName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercises_search);
        etSearch = findViewById(R.id.et_search);
        ivBack = findViewById(R.id.iv_back);
        ivClear = findViewById(R.id.iv_clear);
        rvResult = findViewById(R.id.rv_search_result);
        tvEmpty = findViewById(R.id.tv_empty);
        flowHot = findViewById(R.id.flow_hot);
        flowHistory = findViewById(R.id.flow_history);
        tvClearHistory = findViewById(R.id.tv_clear_history);
        sp = getSharedPreferences(SP_HISTORY, MODE_PRIVATE);
        userName = AnalysisUtils.readLoginUserName(this);
        // 获取全部测评数据（可通过Intent传递或静态方法获取，这里用静态模拟）
        allList = getAllExercises();
        adapter = new ExercisesAdapter(this, false); // 关键：不显示header
        adapter.setAllData(allList); // 设置全量数据供filter用
        adapter.setData(new ArrayList<>()); // 初始不显示任何测评卡片
        rvResult.setLayoutManager(new LinearLayoutManager(this));
        rvResult.setAdapter(adapter);
        rvResult.setVisibility(View.GONE); // 初始隐藏
        // 顶部返回
        ivBack.setOnClickListener(v -> finish());
        // 清除输入
        ivClear.setOnClickListener(v -> etSearch.setText(""));
        // 输入框自动获取焦点并弹出键盘
        etSearch.requestFocus();
        etSearch.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
        // 热搜标签
        flowHot.removeAllViews();
        for (String word : hotWords) {
            TextView tag = createTag(word, 0xFF30B4FF, 0x1A30B4FF);
            tag.setOnClickListener(v -> {
                etSearch.setText(word);
                etSearch.setSelection(word.length());
            });
            flowHot.addView(tag);
        }
        // 历史标签
        showHistory();
        tvClearHistory.setOnClickListener(v -> {
            sp.edit().remove("history_" + userName).apply();
            showHistory();
        });
        // 输入监听实时搜索
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                ivClear.setVisibility(keyword.isEmpty() ? View.GONE : View.VISIBLE);
                if (keyword.isEmpty()) {
                    adapter.setData(new ArrayList<>()); // 清空测评卡片
                    rvResult.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.GONE);
                    findViewById(R.id.ll_hot).setVisibility(View.VISIBLE);
                    findViewById(R.id.ll_history).setVisibility(View.VISIBLE);
                } else {
                    adapter.filter(keyword);
                    rvResult.setVisibility(View.VISIBLE);
                    findViewById(R.id.ll_hot).setVisibility(View.GONE);
                    findViewById(R.id.ll_history).setVisibility(View.GONE);
                    updateEmptyView();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        // 搜索时保存历史
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String keyword = etSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                saveHistory(keyword);
                showHistory();
            }
            return false;
        });
        // 首次进入不显示清除按钮
        ivClear.setVisibility(View.GONE);
        updateEmptyView();
    }
    private void updateEmptyView() {
        if (adapter.getItemCount() <= 1) { // 只有header
            tvEmpty.setVisibility(View.VISIBLE);
            rvResult.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvResult.setVisibility(View.VISIBLE);
        }
    }
    // 可根据实际情况获取全部测评数据
    private List<ExercisesBean> getAllExercises() {
        List<ExercisesBean> ebl = new ArrayList<>();
        // 1. 测测你的恋爱心理成熟度
        ExercisesBean bean1 = new ExercisesBean();
        bean1.id = 1;
        bean1.title = "测测你的恋爱心理成熟度";
        bean1.content = "共计6题";
        bean1.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean1.id);
        bean1.category = "情感";
        List<com.example.xinqiao.bean.QuestionBean> q1 = new ArrayList<>();
        q1.add(new com.example.xinqiao.bean.QuestionBean("你在恋爱中更看重什么？", new String[]{"安全感", "激情", "陪伴", "成长"}, 0));
        q1.add(new com.example.xinqiao.bean.QuestionBean("遇到分歧时你通常会？", new String[]{"主动沟通", "冷处理", "顺其自然", "寻求朋友帮助"}, 0));
        q1.add(new com.example.xinqiao.bean.QuestionBean("你对恋人最大的期待是？", new String[]{"理解和包容", "浪漫惊喜", "共同进步", "经济支持"}, 0));
        q1.add(new com.example.xinqiao.bean.QuestionBean("你是否容易因小事和恋人争吵？", new String[]{"很少", "偶尔", "经常", "总是"}, 0));
        q1.add(new com.example.xinqiao.bean.QuestionBean("你会主动表达自己的情感吗？", new String[]{"经常", "偶尔", "很少", "几乎不"}, 0));
        q1.add(new com.example.xinqiao.bean.QuestionBean("你认为恋爱中最重要的品质是？", new String[]{"信任", "忠诚", "独立", "包容"}, 0));
        bean1.questions = q1;
        ebl.add(bean1);
        // 2. 社交恐惧症量表(SPIN)
        ExercisesBean bean2 = new ExercisesBean();
        bean2.id = 2;
        bean2.title = "社交恐惧症量表(SPIN)";
        bean2.content = "共计5题";
        bean2.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean2.id);
        bean2.category = "人际";
        List<com.example.xinqiao.bean.QuestionBean> q2 = new ArrayList<>();
        q2.add(new com.example.xinqiao.bean.QuestionBean("你在陌生人面前说话会感到紧张吗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q2.add(new com.example.xinqiao.bean.QuestionBean("你是否害怕在公共场合被注视？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q2.add(new com.example.xinqiao.bean.QuestionBean("你会因为害怕尴尬而避免社交活动吗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q2.add(new com.example.xinqiao.bean.QuestionBean("你在小组讨论时会主动发言吗？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q2.add(new com.example.xinqiao.bean.QuestionBean("你是否担心自己的表现会被别人评价？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        bean2.questions = q2;
        ebl.add(bean2);
        // 3. 测测你的交友能力
        ExercisesBean bean3 = new ExercisesBean();
        bean3.id = 3;
        bean3.title = "测测你的交友能力";
        bean3.content = "共计4题";
        bean3.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean3.id);
        bean3.category = "人际";
        List<com.example.xinqiao.bean.QuestionBean> q3 = new ArrayList<>();
        q3.add(new com.example.xinqiao.bean.QuestionBean("你是否容易和陌生人搭话？", new String[]{"很容易", "一般", "有点难", "很难"}, 0));
        q3.add(new com.example.xinqiao.bean.QuestionBean("你有几个可以倾诉的朋友？", new String[]{"很多", "几个", "很少", "没有"}, 0));
        q3.add(new com.example.xinqiao.bean.QuestionBean("你会主动组织聚会或活动吗？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q3.add(new com.example.xinqiao.bean.QuestionBean("遇到矛盾时你会如何处理？", new String[]{"主动沟通", "回避", "冷战", "求助他人"}, 0));
        bean3.questions = q3;
        ebl.add(bean3);
        // 4. 汉密顿抑郁量表–HRSD
        ExercisesBean bean4 = new ExercisesBean();
        bean4.id = 4;
        bean4.title = "汉密顿抑郁量表–HRSD";
        bean4.content = "共计6题";
        bean4.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean4.id);
        bean4.category = "健康";
        List<com.example.xinqiao.bean.QuestionBean> q4 = new ArrayList<>();
        q4.add(new com.example.xinqiao.bean.QuestionBean("你最近是否经常感到情绪低落？", new String[]{"没有", "轻度", "中度", "重度"}, 0));
        q4.add(new com.example.xinqiao.bean.QuestionBean("你是否对平时感兴趣的事物失去兴趣？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        q4.add(new com.example.xinqiao.bean.QuestionBean("你是否经常感到疲惫或没有精力？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        q4.add(new com.example.xinqiao.bean.QuestionBean("你的睡眠质量如何？", new String[]{"很好", "一般", "较差", "很差"}, 0));
        q4.add(new com.example.xinqiao.bean.QuestionBean("你是否有自责或无价值感？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        q4.add(new com.example.xinqiao.bean.QuestionBean("你是否有自杀念头？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
        bean4.questions = q4;
        ebl.add(bean4);
        // 5. 焦虑程度测试
        ExercisesBean bean5 = new ExercisesBean();
        bean5.id = 5;
        bean5.title = "焦虑程度测试";
        bean5.content = "共计5题";
        bean5.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean5.id);
        bean5.category = "健康";
        List<com.example.xinqiao.bean.QuestionBean> q5 = new ArrayList<>();
        q5.add(new com.example.xinqiao.bean.QuestionBean("你是否经常感到紧张或坐立不安？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new com.example.xinqiao.bean.QuestionBean("你是否容易为小事担忧？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new com.example.xinqiao.bean.QuestionBean("你是否经常感到心慌或出汗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new com.example.xinqiao.bean.QuestionBean("你是否会因为担心而影响睡眠？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q5.add(new com.example.xinqiao.bean.QuestionBean("你是否觉得难以控制自己的焦虑情绪？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        bean5.questions = q5;
        ebl.add(bean5);
        // 6. 抑郁症程度测试
        ExercisesBean bean6 = new ExercisesBean();
        bean6.id = 6;
        bean6.title = "抑郁症程度测试";
        bean6.content = "共计5题";
        bean6.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean6.id);
        bean6.category = "健康";
        List<com.example.xinqiao.bean.QuestionBean> q6 = new ArrayList<>();
        q6.add(new com.example.xinqiao.bean.QuestionBean("你是否经常感到情绪低落？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new com.example.xinqiao.bean.QuestionBean("你是否对生活失去兴趣？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new com.example.xinqiao.bean.QuestionBean("你是否经常感到疲惫或无力？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new com.example.xinqiao.bean.QuestionBean("你是否经常自责或觉得自己没用？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q6.add(new com.example.xinqiao.bean.QuestionBean("你是否有过自杀的念头？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        bean6.questions = q6;
        ebl.add(bean6);
        // 7. 精神压力程度测试
        ExercisesBean bean7 = new ExercisesBean();
        bean7.id = 7;
        bean7.title = "精神压力程度测试";
        bean7.content = "共计4题";
        bean7.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean7.id);
        bean7.category = "健康";
        List<com.example.xinqiao.bean.QuestionBean> q7 = new ArrayList<>();
        q7.add(new com.example.xinqiao.bean.QuestionBean("你是否经常感到压力大？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q7.add(new com.example.xinqiao.bean.QuestionBean("你是否因压力影响睡眠？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q7.add(new com.example.xinqiao.bean.QuestionBean("你是否因压力而情绪波动？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q7.add(new com.example.xinqiao.bean.QuestionBean("你是否有缓解压力的有效方法？", new String[]{"有", "偶尔有", "很少有", "没有"}, 0));
        bean7.questions = q7;
        ebl.add(bean7);
        // 8. 抑郁应对方式评估
        ExercisesBean bean8 = new ExercisesBean();
        bean8.id = 8;
        bean8.title = "抑郁应对方式评估";
        bean8.content = "共计5题";
        bean8.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean8.id);
        bean8.category = "健康";
        List<com.example.xinqiao.bean.QuestionBean> q8 = new ArrayList<>();
        q8.add(new com.example.xinqiao.bean.QuestionBean("你遇到挫折时会？", new String[]{"自我安慰", "寻求帮助", "消极回避", "积极面对"}, 0));
        q8.add(new com.example.xinqiao.bean.QuestionBean("你倾向于如何调节情绪？", new String[]{"运动", "倾诉", "独处", "娱乐"}, 0));
        q8.add(new com.example.xinqiao.bean.QuestionBean("你是否会主动寻求心理咨询？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q8.add(new com.example.xinqiao.bean.QuestionBean("你是否会写日记或记录情绪？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q8.add(new com.example.xinqiao.bean.QuestionBean("你是否愿意与亲友分享自己的困扰？", new String[]{"非常愿意", "偶尔", "很少", "不愿意"}, 0));
        bean8.questions = q8;
        ebl.add(bean8);
        // 9. 回避型依恋测试
        ExercisesBean bean9 = new ExercisesBean();
        bean9.id = 9;
        bean9.title = "回避型依恋测试";
        bean9.content = "共计4题";
        bean9.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean9.id);
        bean9.category = "情感";
        List<com.example.xinqiao.bean.QuestionBean> q9 = new ArrayList<>();
        q9.add(new com.example.xinqiao.bean.QuestionBean("你是否害怕与人建立亲密关系？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
        q9.add(new com.example.xinqiao.bean.QuestionBean("你是否习惯独处？", new String[]{"非常习惯", "偶尔", "不太习惯", "不习惯"}, 0));
        q9.add(new com.example.xinqiao.bean.QuestionBean("你是否会主动疏远亲近的人？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q9.add(new com.example.xinqiao.bean.QuestionBean("你是否觉得依赖别人会让你不安？", new String[]{"非常不安", "有点不安", "无所谓", "不会"}, 0));
        bean9.questions = q9;
        ebl.add(bean9);
        // 10. 人生质量评估
        ExercisesBean bean10 = new ExercisesBean();
        bean10.id = 10;
        bean10.title = "人生质量评估";
        bean10.content = "共计5题";
        bean10.background = com.example.xinqiao.view.ExercisesView.getBackgroundResById(bean10.id);
        bean10.category = "能力";
        List<com.example.xinqiao.bean.QuestionBean> q10 = new ArrayList<>();
        q10.add(new com.example.xinqiao.bean.QuestionBean("你对目前的生活满意吗？", new String[]{"非常满意", "比较满意", "一般", "不满意"}, 0));
        q10.add(new com.example.xinqiao.bean.QuestionBean("你是否有明确的人生目标？", new String[]{"非常明确", "比较明确", "不太明确", "没有"}, 0));
        q10.add(new com.example.xinqiao.bean.QuestionBean("你是否经常感到快乐？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
        q10.add(new com.example.xinqiao.bean.QuestionBean("你是否有良好的人际关系？", new String[]{"非常好", "一般", "较差", "很差"}, 0));
        q10.add(new com.example.xinqiao.bean.QuestionBean("你是否有健康的生活方式？", new String[]{"非常健康", "比较健康", "一般", "不健康"}, 0));
        bean10.questions = q10;
        ebl.add(bean10);
        return ebl;
    }
    private void showHistory() {
        flowHistory.removeAllViews();
        List<String> history = getHistory();
        if (history.isEmpty()) {
            findViewById(R.id.ll_history).setVisibility(View.GONE);
        } else {
            findViewById(R.id.ll_history).setVisibility(View.VISIBLE);
            flowHistory.setVisibility(View.VISIBLE);
            for (String word : history) {
                TextView tag = createTag(word, 0xFFBBBBBB, 0x1A999999);
                tag.setOnClickListener(v -> {
                    etSearch.setText(word);
                    etSearch.setSelection(word.length());
                });
                flowHistory.addView(tag);
            }
        }
    }
    private void saveHistory(String keyword) {
        List<String> history = getHistory();
        history.remove(keyword);
        history.add(0, keyword);
        if (history.size() > MAX_HISTORY) history = history.subList(0, MAX_HISTORY);
        sp.edit().putString("history_" + userName, String.join(",", history)).apply();
    }
    private List<String> getHistory() {
        String str = sp.getString("history_" + userName, "");
        List<String> list = new ArrayList<>();
        if (!str.isEmpty()) {
            for (String s : str.split(",")) {
                if (!s.isEmpty()) list.add(s);
            }
        }
        return list;
    }
    private TextView createTag(String text, int strokeColor, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(strokeColor);
        tv.setTextSize(14);
        tv.setPadding(32, 12, 32, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 24, 0);
        tv.setLayoutParams(lp);
        
        // 创建背景drawable
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(48);
        drawable.setStroke(2, strokeColor);
        drawable.setColor(bgColor);
        
        // 使用mutate()创建drawable的可变副本，避免共享状态
        drawable.mutate();
        
        // 设置drawable为不可缓存
        drawable.setCallback(null);
        
        // 设置背景
        tv.setBackground(drawable);
        
        return tv;
    }
}