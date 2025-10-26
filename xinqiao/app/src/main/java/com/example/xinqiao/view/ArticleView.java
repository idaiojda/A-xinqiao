package com.example.xinqiao.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.ArticleAdapter;
import com.example.xinqiao.bean.ArticleBean;
import com.example.xinqiao.dao.ArticleHistoryDao;
import com.example.xinqiao.utils.AnalysisUtils;

import java.util.ArrayList;
import java.util.List;

public class ArticleView extends FrameLayout implements View.OnClickListener {

    private View mView;
    private Context mContext;
    private TextView tvAllArticles, tvReadHistory;
    private RecyclerView rvArticleList;
    private ArticleAdapter mAdapter;
    private ArticleHistoryDao articleHistoryDao;
    private List<ArticleBean> mArticleList;
    private boolean isShowingHistory = false;

    public ArticleView(Context context) {
        super(context);
        mContext = context;
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.main_view_article, this, true);
        
        // 初始化控件
        tvAllArticles = findViewById(R.id.tv_all_articles);
        tvReadHistory = findViewById(R.id.tv_read_history);
        rvArticleList = findViewById(R.id.rv_article_list);
        
        // 设置点击事件
        tvAllArticles.setOnClickListener(this);
        tvReadHistory.setOnClickListener(this);
        
        // 初始化RecyclerView
        rvArticleList.setLayoutManager(new LinearLayoutManager(context));
        mArticleList = new ArrayList<>();
        mAdapter = new ArticleAdapter(context, mArticleList, false);
        rvArticleList.setAdapter(mAdapter);
        
        // 初始化数据库操作类
        articleHistoryDao = new ArticleHistoryDao(context);
        
        // 设置文章点击事件
        mAdapter.setOnItemClickListener(article -> {
            // TODO: 跳转到文章详情页面
            // 这里需要实现文章详情页面的跳转逻辑
        });
        
        // 加载全部文章列表
        loadAllArticles();
    }

    public View getView() {
        return mView;
    }

    public void showView() {
        if (mView == null) {
            init(getContext());
        }
        if (mView != null) {
            mView.setVisibility(View.VISIBLE);
            // 刷新数据
            if (isShowingHistory) {
                loadReadHistory();
            } else {
                loadAllArticles();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_all_articles) {
            if (isShowingHistory) {
                isShowingHistory = false;
                updateTabStyle();
                loadAllArticles();
            }
        } else if (v.getId() == R.id.tv_read_history) {
            if (!isShowingHistory) {
                isShowingHistory = true;
                updateTabStyle();
                loadReadHistory();
            }
        }
    }

    private void updateTabStyle() {
        if (isShowingHistory) {
            tvAllArticles.setTextColor(Color.parseColor("#666666"));
            tvReadHistory.setTextColor(Color.parseColor("#0097F7"));
            mAdapter = new ArticleAdapter(mContext, mArticleList, true);
        } else {
            tvAllArticles.setTextColor(Color.parseColor("#0097F7"));
            tvReadHistory.setTextColor(Color.parseColor("#666666"));
            mAdapter = new ArticleAdapter(mContext, mArticleList, false);
        }
        rvArticleList.setAdapter(mAdapter);
    }

    private void loadAllArticles() {
        mArticleList = new ArrayList<>();
        // 心理健康相关文章
        String[] titles = {
            "如何应对焦虑情绪",
            "提升自尊的五个方法",
            "压力管理：科学减压技巧",
            "心理健康与睡眠质量的关系",
            "社交恐惧的自助指南",
            "青少年心理健康的关键点",
            "如何与抑郁情绪共处",
            "正念冥想的心理益处",
            "亲密关系中的沟通技巧",
            "远离网络成瘾，守护心理健康"
        };
        String[] categories = {
            "情绪管理", "自我成长", "压力管理", "健康生活", "社交心理",
            "青少年", "抑郁应对", "正念冥想", "亲密关系", "网络心理"
        };
        String[] summaries = {
            "焦虑是常见的情绪体验，学会识别和接纳焦虑，掌握呼吸放松等技巧，有助于缓解焦虑带来的困扰。",
            "自尊影响我们的幸福感。通过自我肯定、设立小目标、积极社交等方法，可以逐步提升自信和自我价值感。",
            "压力无处不在，科学的减压方法如运动、冥想、时间管理等，能帮助我们更好地应对生活挑战。",
            "良好的睡眠是心理健康的重要保障。建立规律作息、营造舒适环境，有助于提升睡眠质量。",
            "社交恐惧影响人际交往。通过暴露疗法、正念练习和逐步挑战社交场合，可以逐步克服恐惧。",
            "青春期是心理成长的关键阶段。关注情绪变化、学会沟通和寻求支持，有助于青少年健康成长。",
            "抑郁情绪并不可怕，学会自我关怀、寻求专业帮助和与亲友沟通，是走出低谷的重要途径。",
            "正念冥想有助于缓解压力、提升专注力和情绪调节能力，是现代心理健康管理的有效工具。",
            "良好的沟通是亲密关系的基石。学会倾听、表达和共情，有助于增进理解和亲密感。",
            "网络成瘾影响身心健康。合理规划上网时间，培养多样兴趣，有助于保持心理平衡。"
        };
        String[] contents = {
            "焦虑是一种常见的情绪体验，适度的焦虑有助于我们应对挑战，但过度焦虑会影响生活。可以通过深呼吸、肌肉放松、正念冥想等方法缓解焦虑，必要时可寻求心理咨询师的帮助。",
            "自尊是对自我价值的肯定。提升自尊可以从自我接纳、积极自我对话、设立可实现的小目标、参与社交活动等方面入手。遇到挫折时要善于自我鼓励。",
            "压力管理包括识别压力源、合理安排时间、保持运动习惯、学会放松和寻求社会支持。遇到压力大时，不妨尝试冥想或与朋友倾诉。",
            "睡眠与心理健康密切相关。保持规律作息、睡前减少电子产品使用、营造安静环境，有助于改善睡眠。长期失眠建议寻求专业帮助。",
            "社交恐惧常表现为害怕被关注或评价。可以通过逐步暴露、正念练习、记录进步等方式克服，必要时可寻求心理咨询。",
            "青少年时期情绪波动大，家长和老师应多关注其心理变化。青少年自身要学会表达情绪、主动沟通和寻求帮助。",
            "抑郁情绪可能表现为持续低落、兴趣减退等。可以通过规律作息、适度运动、与亲友交流等方式缓解，严重时应及时就医。",
            "正念冥想是一种专注于当下的练习，有助于缓解压力、提升情绪调节能力。每天坚持几分钟正念练习，对心理健康大有裨益。",
            "亲密关系中的沟通应以尊重和理解为基础。学会倾听、表达真实感受和需求，有助于减少误会和冲突。",
            "网络成瘾会影响学习和生活。可以通过设定上网时间、培养线下兴趣、增加户外活动等方式预防和改善。"
        };
        int[] imageResIds = {
            R.drawable.bg_11, R.drawable.bg_12, R.drawable.bg_13, R.drawable.bg_14, R.drawable.bg_15,
            R.drawable.bg_16, R.drawable.bg_17, R.drawable.bg_18, R.drawable.bg_19, R.drawable.bg_20
        };
        for (int i = 0; i < titles.length; i++) {
            ArticleBean article = new ArticleBean();
            article.articleId = i + 1;
            article.title = titles[i];
            article.category = categories[i];
            article.summary = summaries[i];
            article.content = contents[i];
            article.imageResId = imageResIds[i];
            article.readTimestamp = System.currentTimeMillis();
            mArticleList.add(article);
        }
        mAdapter.setData(mArticleList);
    }

    private void loadReadHistory() {
        String userName = AnalysisUtils.readLoginUserName(mContext);
        if (userName != null && !userName.isEmpty()) {
            articleHistoryDao.getArticleHistoryAsync(userName, history -> {
                mArticleList = history;
                mAdapter.setData(mArticleList);
            });
        }
    }
}
