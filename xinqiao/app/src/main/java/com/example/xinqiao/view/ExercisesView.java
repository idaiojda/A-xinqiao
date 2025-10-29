package com.example.xinqiao.view;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.MainActivity;
import com.example.xinqiao.adapter.ExercisesAdapter;
import com.example.xinqiao.adapter.RecommendAdapter;
import com.example.xinqiao.bean.ExercisesBean;
import com.example.xinqiao.bean.QuestionBean;
import com.example.xinqiao.fragment.TestRecordFragment;

import java.util.ArrayList;
import java.util.List;

public class ExercisesView {
    private RecyclerView rv_list;
    private ExercisesAdapter adapter;
    private List<ExercisesBean> ebl;
    private Activity mContext;
    private LayoutInflater mInflater;
    private View mCurrentView;
    private List<RecommendAdapter.RecommendBean> recommendList;
    public ExercisesView(Activity context) {
        mContext = context;
        // 为之后将Layout转化为view时用
        mInflater = LayoutInflater.from(mContext);
    }
    private void createView() {
        initView();
    }
    /**
     * 初始化控件
     */
    private void initView() {
        mCurrentView = mInflater
                .inflate(R.layout.main_view_exercises, null);
        // 初始化主RecyclerView
        rv_list = mCurrentView.findViewById(R.id.rv_list);
        rv_list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(mContext));
        adapter = new ExercisesAdapter(mContext);
        // 推荐数据
        recommendList = new ArrayList<>();
        recommendList.add(new RecommendAdapter.RecommendBean(1, "测测你的恋爱心理成熟度", "2.6万人测过", getBackgroundResById(1)));
        recommendList.add(new RecommendAdapter.RecommendBean(2, "社交恐惧症量表(SPIN)", "2.8万人测过", getBackgroundResById(2)));
        recommendList.add(new RecommendAdapter.RecommendBean(3, "测测你的交友能力", "25.1万人测过", getBackgroundResById(3)));
        recommendList.add(new RecommendAdapter.RecommendBean(4, "汉密顿抑郁量表–HRSD", "263.4万人测过", getBackgroundResById(4)));
        recommendList.add(new RecommendAdapter.RecommendBean(5, "焦虑程度测试", "1.2万人测过", getBackgroundResById(5)));
        recommendList.add(new RecommendAdapter.RecommendBean(6, "抑郁症程度测试", "3.4万人测过", getBackgroundResById(6)));
        recommendList.add(new RecommendAdapter.RecommendBean(7, "精神压力程度测试", "4.5万人测过", getBackgroundResById(7)));
        recommendList.add(new RecommendAdapter.RecommendBean(8, "抑郁应对方式评估", "2.1万人测过", getBackgroundResById(8)));
        recommendList.add(new RecommendAdapter.RecommendBean(9, "回避型依恋测试", "1.8万人测过", getBackgroundResById(9)));
        recommendList.add(new RecommendAdapter.RecommendBean(10, "人生质量评估", "5.6万人测过", getBackgroundResById(10)));
        adapter.setRecommendData(recommendList);
        // 习题数据
        initData();
        adapter.setData(ebl);
        rv_list.setAdapter(adapter);
        // 绑定"我的测评"点击事件
        TextView tvMyTest = mCurrentView.findViewById(R.id.tv_my_test);
        tvMyTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMyTestClick(v);
            }
        });
        ImageView ivMyTest = mCurrentView.findViewById(R.id.iv_my_test);
        ivMyTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMyTestClick(v);
            }
        });
    }
    /**
     * 设置数据
     */
    private void initData() {
        ebl = new ArrayList<>();
        // 1. 测测你的恋爱心理成熟度 (免费)
        ExercisesBean bean1 = new ExercisesBean();
        bean1.id = 1;
        bean1.title = "测测你的恋爱心理成熟度";
        bean1.content = "共计6题";
        bean1.background = R.drawable.bg_1;
        bean1.isPremium = false;
        bean1.price = 0.0;
        bean1.isPurchased = false;
        List<QuestionBean> q1 = new ArrayList<>();
        q1.add(newQuestion("你在恋爱中更看重什么？", "安全感", "激情", "陪伴", "成长", 0));
        q1.add(newQuestion("遇到分歧时你通常会？", "主动沟通", "冷处理", "顺其自然", "寻求朋友帮助", 0));
        q1.add(newQuestion("你对恋人最大的期待是？", "理解和包容", "浪漫惊喜", "共同进步", "经济支持", 0));
        q1.add(newQuestion("你是否容易因小事和恋人争吵？", "很少", "偶尔", "经常", "总是", 0));
        q1.add(newQuestion("你会主动表达自己的情感吗？", "经常", "偶尔", "很少", "几乎不", 0));
        q1.add(newQuestion("你认为恋爱中最重要的品质是？", "信任", "忠诚", "独立", "包容", 0));
        bean1.questions = q1;
        ebl.add(bean1);
        

        // 2. 社交恐惧症量表(SPIN) (免费)
        ExercisesBean bean2 = new ExercisesBean();
        bean2.id = 2;
        bean2.title = "社交恐惧症量表(SPIN)";
        bean2.content = "共计5题";
        bean2.background = R.drawable.bg_2;
        bean2.isPremium = false;
        bean2.price = 0.0;
        bean2.isPurchased = false;
        List<QuestionBean> q2 = new ArrayList<>();
        q2.add(newQuestion("你在陌生人面前说话会感到紧张吗？", "从不", "偶尔", "经常", "总是", 0));
        q2.add(newQuestion("你是否害怕在公共场合被注视？", "从不", "偶尔", "经常", "总是", 0));
        q2.add(newQuestion("你会因为害怕尴尬而避免社交活动吗？", "从不", "偶尔", "经常", "总是", 0));
        q2.add(newQuestion("你在小组讨论时会主动发言吗？", "经常", "偶尔", "很少", "从不", 0));
        q2.add(newQuestion("你是否担心自己的表现会被别人评价？", "从不", "偶尔", "经常", "总是", 0));
        bean2.questions = q2;
        ebl.add(bean2);
        // 3. 测测你的交友能力 (付费)
        ExercisesBean bean3 = new ExercisesBean();
        bean3.id = 3;
        bean3.title = "测测你的交友能力";
        bean3.content = "专业社交能力评估";
        bean3.background = R.drawable.bg_3;
        bean3.isPremium = true;
        bean3.price = 9.9;
        bean3.isPurchased = false;
        List<QuestionBean> q3 = new ArrayList<>();
        q3.add(newQuestion("你是否容易和陌生人搭话？", "很容易", "一般", "有点难", "很难", 0));
        q3.add(newQuestion("你有几个可以倾诉的朋友？", "很多", "几个", "很少", "没有", 0));
        q3.add(newQuestion("你会主动组织聚会或活动吗？", "经常", "偶尔", "很少", "从不", 0));
        q3.add(newQuestion("遇到矛盾时你会如何处理？", "主动沟通", "回避", "冷战", "求助他人", 0));
        bean3.questions = q3;
        ebl.add(bean3);
        // 4. 汉密顿抑郁量表–HRSD (付费)
        ExercisesBean bean4 = new ExercisesBean();
        bean4.id = 4;
        bean4.title = "汉密顿抑郁量表–HRSD";
        bean4.content = "专业抑郁评估量表";
        bean4.background = R.drawable.bg_4;
        bean4.isPremium = true;
        bean4.price = 15.9;
        bean4.isPurchased = false;
        List<QuestionBean> q4 = new ArrayList<>();
        q4.add(newQuestion("你最近是否经常感到情绪低落？", "没有", "轻度", "中度", "重度", 0));
        q4.add(newQuestion("你是否对平时感兴趣的事物失去兴趣？", "没有", "偶尔", "经常", "总是", 0));
        q4.add(newQuestion("你是否经常感到疲惫或没有精力？", "没有", "偶尔", "经常", "总是", 0));
        q4.add(newQuestion("你的睡眠质量如何？", "很好", "一般", "较差", "很差", 0));
        q4.add(newQuestion("你是否有自责或无价值感？", "没有", "偶尔", "经常", "总是", 0));
        q4.add(newQuestion("你是否有自杀念头？", "没有", "偶尔", "经常", "总是", 0));
        bean4.questions = q4;
        ebl.add(bean4);
        // 5. 焦虑程度测试 (付费)
        ExercisesBean bean5 = new ExercisesBean();
        bean5.id = 5;
        bean5.title = "焦虑程度测试";
        bean5.content = "专业心理评估量表";
        bean5.background = R.drawable.bg_5;
        bean5.isPremium = true;
        bean5.price = 19.9;
        bean5.isPurchased = false;
        List<QuestionBean> q5 = new ArrayList<>();
        q5.add(newQuestion("你是否经常感到紧张或坐立不安？", "从不", "偶尔", "经常", "总是", 0));
        q5.add(newQuestion("你是否容易为小事担忧？", "从不", "偶尔", "经常", "总是", 0));
        q5.add(newQuestion("你是否经常感到心慌或出汗？", "从不", "偶尔", "经常", "总是", 0));
        q5.add(newQuestion("你是否会因为担心而影响睡眠？", "从不", "偶尔", "经常", "总是", 0));
        q5.add(newQuestion("你是否觉得难以控制自己的焦虑情绪？", "从不", "偶尔", "经常", "总是", 0));
        bean5.questions = q5;
        ebl.add(bean5);
        // 6. 抑郁症程度测试 (免费)
        ExercisesBean bean6 = new ExercisesBean();
        bean6.id = 6;
        bean6.title = "抑郁症程度测试";
        bean6.content = "共计5题";
        bean6.background = R.drawable.bg_6;
        bean6.isPremium = false;
        bean6.price = 0.0;
        bean6.isPurchased = false;
        List<QuestionBean> q6 = new ArrayList<>();
        q6.add(newQuestion("你是否经常感到情绪低落？", "从不", "偶尔", "经常", "总是", 0));
        q6.add(newQuestion("你是否对生活失去兴趣？", "从不", "偶尔", "经常", "总是", 0));
        q6.add(newQuestion("你是否经常感到疲惫或无力？", "从不", "偶尔", "经常", "总是", 0));
        q6.add(newQuestion("你是否经常自责或觉得自己没用？", "从不", "偶尔", "经常", "总是", 0));
        q6.add(newQuestion("你是否有过自杀的念头？", "从不", "偶尔", "经常", "总是", 0));
        bean6.questions = q6;
        ebl.add(bean6);
        // 7. 精神压力程度测试 (付费)
        ExercisesBean bean7 = new ExercisesBean();
        bean7.id = 7;
        bean7.title = "精神压力程度测试";
        bean7.content = "专业压力评估量表";
        bean7.background = R.drawable.bg_7;
        bean7.isPremium = true;
        bean7.price = 12.9;
        bean7.isPurchased = false;
        List<QuestionBean> q7 = new ArrayList<>();
        q7.add(newQuestion("你是否经常感到压力大？", "从不", "偶尔", "经常", "总是", 0));
        q7.add(newQuestion("你是否因压力影响睡眠？", "从不", "偶尔", "经常", "总是", 0));
        q7.add(newQuestion("你是否因压力而情绪波动？", "从不", "偶尔", "经常", "总是", 0));
        q7.add(newQuestion("你是否有缓解压力的有效方法？", "有", "偶尔有", "很少有", "没有", 0));
        bean7.questions = q7;
        ebl.add(bean7);
        // 8. 抑郁应对方式评估 (付费)
        ExercisesBean bean8 = new ExercisesBean();
        bean8.id = 8;
        bean8.title = "抑郁应对方式评估";
        bean8.content = "专业心理应对策略";
        bean8.background = R.drawable.bg_8;
        bean8.isPremium = true;
        bean8.price = 14.9;
        bean8.isPurchased = false;
        List<QuestionBean> q8 = new ArrayList<>();
        q8.add(newQuestion("你遇到挫折时会？", "自我安慰", "寻求帮助", "消极回避", "积极面对", 0));
        q8.add(newQuestion("你倾向于如何调节情绪？", "运动", "倾诉", "独处", "娱乐", 0));
        q8.add(newQuestion("你是否会主动寻求心理咨询？", "经常", "偶尔", "很少", "从不", 0));
        q8.add(newQuestion("你是否会写日记或记录情绪？", "经常", "偶尔", "很少", "从不", 0));
        q8.add(newQuestion("你是否愿意与亲友分享自己的困扰？", "非常愿意", "偶尔", "很少", "不愿意", 0));
        bean8.questions = q8;
        ebl.add(bean8);
        // 9. 回避型依恋测试 (免费)
        ExercisesBean bean9 = new ExercisesBean();
        bean9.id = 9;
        bean9.title = "回避型依恋测试";
        bean9.content = "共计4题";
        bean9.background = R.drawable.bg_9;
        bean9.isPremium = false;
        bean9.price = 0.0;
        bean9.isPurchased = false;
        List<QuestionBean> q9 = new ArrayList<>();
        q9.add(newQuestion("你是否害怕与人建立亲密关系？", "从不", "偶尔", "经常", "总是", 0));
        q9.add(newQuestion("你是否习惯独处？", "非常习惯", "偶尔", "不太习惯", "不习惯", 0));
        q9.add(newQuestion("你是否会主动疏远亲近的人？", "经常", "偶尔", "很少", "从不", 0));
        q9.add(newQuestion("你是否觉得依赖别人会让你不安？", "非常不安", "有点不安", "无所谓", "不会", 0));
        bean9.questions = q9;
        ebl.add(bean9);
        // 10. 人生质量评估 (付费)
        ExercisesBean bean10 = new ExercisesBean();
        bean10.id = 10;
        bean10.title = "人生质量评估";
        bean10.content = "专业生活质量分析";
        bean10.background = R.drawable.bg_10;
        bean10.isPremium = true;
        bean10.price = 16.9;
        bean10.isPurchased = false;
        List<QuestionBean> q10 = new ArrayList<>();
        q10.add(newQuestion("你对目前的生活满意吗？", "非常满意", "比较满意", "一般", "不满意", 0));
        q10.add(newQuestion("你是否有明确的人生目标？", "非常明确", "比较明确", "不太明确", "没有", 0));
        q10.add(newQuestion("你是否经常感到快乐？", "经常", "偶尔", "很少", "从不", 0));
        q10.add(newQuestion("你是否有良好的人际关系？", "非常好", "一般", "较差", "很差", 0));
        q10.add(newQuestion("你是否有健康的生活方式？", "非常健康", "比较健康", "一般", "不健康", 0));
        bean10.questions = q10;
        ebl.add(bean10);
    }

    // 工具方法：快速创建题目
    private QuestionBean newQuestion(String q, String a, String b, String c, String d, int correct) {
        QuestionBean qb = new QuestionBean();
        qb.question = q;
        qb.options = new String[]{a, b, c, d};
        qb.correctIndex = correct;
        return qb;
    }
    /**
     * 获取当前在导航栏上方显示对应的View
     */
    public View getView() {
        if (mCurrentView == null) {
            createView();
        }
        return mCurrentView;
    }
    /**
     * 显示当前导航栏上方所对应的view界面
     */
    public void showView() {
        if (mCurrentView == null) {
            createView();
        }
        mCurrentView.setVisibility(View.VISIBLE);
        // 刷新推荐数据以确保图片正确显示
        if (adapter != null) {
            // 重新设置推荐数据以强制刷新图片
            if (recommendList != null) {
                adapter.setRecommendData(recommendList);
            }
            // 强制刷新适配器
            adapter.notifyDataSetChanged();
        }
    }

    public void onMyTestClick(View v) {
        MainActivity mainActivity = (MainActivity) mContext;
        // 不再隐藏当前视图，避免返回时出现空白（Fragment出栈后自动显示底层视图）
        // View currentView = this.getView();
        // if (currentView != null) {
        //     currentView.setVisibility(View.GONE);
        // }

        // 创建并添加新的Fragment
        FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        
        // 创建并添加新的Fragment
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        TestRecordFragment testRecordFragment = new TestRecordFragment();
        transaction.replace(mainActivity.getBodyLayout().getId(), testRecordFragment, "TestRecordFragment");
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    // 统一测评图片资源数组
    public static final int[] EXERCISE_IMAGES = {
        R.drawable.bg_1, R.drawable.bg_2, R.drawable.bg_3, R.drawable.bg_4,
        R.drawable.bg_5, R.drawable.bg_6, R.drawable.bg_7, R.drawable.bg_8,
        R.drawable.bg_9, R.drawable.bg_10
    };

    // 静态方法：根据id获取推荐区图片资源
    public static int getBackgroundResById(int id) {
        if (id >= 1 && id <= EXERCISE_IMAGES.length) {
            return EXERCISE_IMAGES[id - 1];
        }
        return R.drawable.bg_1;
    }
}
