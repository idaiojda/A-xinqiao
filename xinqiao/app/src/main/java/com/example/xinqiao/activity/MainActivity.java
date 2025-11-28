package com.example.xinqiao.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.xinqiao.R;
import com.example.xinqiao.fragment.TestRecordFragment;
import com.example.xinqiao.fragment.CourseSearchFragment;
import com.example.xinqiao.view.ArticleView;
import com.example.xinqiao.view.CourseView;
import com.example.xinqiao.view.ExercisesView;
import com.example.xinqiao.view.MyInfoView;
import com.example.xinqiao.view.ConsultationView;
import com.example.xinqiao.view.CommunityView;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.mysql.DBUtils;
import com.example.xinqiao.network.NetworkConfig;
import com.example.xinqiao.community.CommunityServiceFactory;
import com.example.xinqiao.community.CommunityApi;
import com.example.xinqiao.community.RemoteCommunityRepository;
import com.example.xinqiao.community.CommunityRepositoryProvider;
import com.example.xinqiao.community.CommunityLocalCache;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    /**
     * 视图
     */
    private CourseView mCourseView;
    private ExercisesView mExercisesView;
    private MyInfoView mMyInfoView;
    private ArticleView mArticleView;
    private ConsultationView mConsultationView;
    private CommunityView mCommunityView;
    /**
     * 中间内容栏
     */
    private FrameLayout mBodyLayout;
    /**
     * 底部按钮栏
     */
    public LinearLayout mBottomLayout;
    /**
     * 底部按钮
     */
    private View mCourseBtn;
    private View mExercisesBtn;
    private View mMyInfoBtn;
    private View mArticleBtn;
    private View mAiBtn;
    private View mCommunityBtn;
    private TextView tv_course;
    private TextView tv_exercises;
    private TextView tv_myInfo;
    private TextView tv_article;
    private TextView tv_ai;
    private TextView tv_community;
    private ImageView iv_course;
    private ImageView iv_exercises;
    private ImageView iv_myInfo;
    private ImageView iv_article;
    private ImageView iv_ai;
    private ImageView iv_community;
    private TextView tv_back;
    private TextView tv_main_title;
    private RelativeLayout rl_title_bar;
    // 课程页顶部搜索与播放历史
    private EditText et_search_course;
    private View ll_play_history_top;
    // 用于后台任务的线程池
    private ExecutorService executorService;
    // 记录当前显示的视图索引（0:课程,1:习题,2:我,3:文章,4:咨询）
    private int currentIndex = -1;
    // 简单界面返回栈：记录从哪个视图跳转到当前视图
    private java.util.ArrayDeque<Integer> viewHistory = new java.util.ArrayDeque<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //设置此界面为竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        CommunityLocalCache.INSTANCE.init(this);

        // 社区仓库懒加载到社区页面，当前保留本地仓库，减少冷启动耗时

        // 初始化线程池
        executorService = Executors.newFixedThreadPool(2);

        // 初始化数据库
        MySQLHelper.getInstance(this, new MySQLHelper.InitCallback() {
            @Override
            public void onSuccess() {
                // 数据库表已在XinQiaoApplication中创建，此处不再重复创建
                Log.d("MainActivity", "数据库连接初始化成功");
            }

            @Override
            public void onError(SQLException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "数据库连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        // 初始化DBUtils，确保helper不为null
        DBUtils.init(this, null);

        init();
        initBottomBar();
        setListener();

        if (savedInstanceState != null) {
            // 恢复保存的状态
            restoreState(savedInstanceState);
        } else {
            // 深链处理：根据 Intent 指定打开测试记录页
            String open = getIntent() != null ? getIntent().getStringExtra("open") : null;
            if ("test_records".equals(open)) {
                mBodyLayout.post(() -> {
                    try {
                        // 先切到习题视图，确保底部导航与标题状态正确
                        clearBottomImageState();
                        setSelectedStatus(1);
                        createView(1);
                        currentIndex = 1;
                        // 打开测试记录 Fragment
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        TestRecordFragment testRecordFragment = new TestRecordFragment();
                        transaction.replace(getBodyLayout().getId(), testRecordFragment, "TestRecordFragment");
                        transaction.addToBackStack(null);
                        transaction.commitAllowingStateLoss();
                        
                        // 隐藏底部导航栏
                        if (mBottomLayout != null) {
                            mBottomLayout.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Deep link to TestRecordFragment failed: " + e.getMessage());
                        // 回退到默认视图
                        mBodyLayout.postDelayed(() -> setInitStatus(), 100);
                    }
                });
            } else if ("consultation".equals(open)) {
                // 直接打开咨询聚合页（包含AI浮窗），不入Fragment栈
                mBodyLayout.post(() -> {
                    try {
                        clearBottomImageState();
                        setSelectedStatus(4);
                        createView(4);
                        currentIndex = 4;
                    } catch (Exception e) {
                        Log.e("MainActivity", "Deep link to ConsultationView failed: " + e.getMessage());
                        mBodyLayout.postDelayed(() -> setInitStatus(), 100);
                    }
                });
            } else {
                // 延迟初始化默认视图，避免阻塞主线程
                mBodyLayout.postDelayed(() -> setInitStatus(), 100);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前视图状态
        if (mCourseView != null && mCourseView.getView().getVisibility() == View.VISIBLE) {
            outState.putInt("current_view", R.id.bottom_bar_course_btn);
        } else if (mExercisesView != null && mExercisesView.getView().getVisibility() == View.VISIBLE) {
            outState.putInt("current_view", R.id.bottom_bar_exercises_btn);
        } else if (mMyInfoView != null && mMyInfoView.getView().getVisibility() == View.VISIBLE) {
            outState.putInt("current_view", R.id.bottom_bar_myinfo_btn);
        } else if (mArticleView != null && mArticleView.getVisibility() == View.VISIBLE) {
            outState.putInt("current_view", R.id.bottom_bar_article_btn);
        } else if (mConsultationView != null && mConsultationView.getView() != null && mConsultationView.getView().getVisibility() == View.VISIBLE) {
            outState.putInt("current_view", R.id.bottom_bar_ai_btn);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        int currentView = savedInstanceState.getInt("current_view", R.id.bottom_bar_course_btn);
        setSelectedStatus(currentView);
        // 延迟恢复视图状态，避免阻塞主线程（不记录历史）
        mBodyLayout.post(() -> selectDisplayViewInternal(currentView, false));
    }

    /**
     * 获取界面上的UI控件
     */
    private void init() {
        tv_back = (TextView) findViewById(R.id.tv_back);
        tv_main_title = (TextView) findViewById(R.id.tv_main_title);
        tv_main_title.setText(getString(R.string.psych_courses));
        rl_title_bar = (RelativeLayout) findViewById(R.id.title_bar);
        tv_back.setVisibility(View.GONE);
        // 顶部课程搜索与播放历史（默认隐藏，课程页显示）
        et_search_course = (EditText) findViewById(R.id.et_search_course);
        ll_play_history_top = findViewById(R.id.ll_play_history_top);
        if (et_search_course != null) et_search_course.setVisibility(View.GONE);
        if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
        // 课程搜索框加入回车/搜索IME行为：跳转到课程搜索页
        if (et_search_course != null) {
            et_search_course.setOnEditorActionListener((v, actionId, event) -> {
                boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
                boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH;
                if (isEnter || isSearchAction) {
                    String query = v.getText() != null ? v.getText().toString().trim() : "";
                    if (query.isEmpty()) {
                        Toast.makeText(MainActivity.this, getString(R.string.search_keyword_empty), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    try {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                    } catch (Exception ignore) {}
                    Intent intent = new Intent(MainActivity.this, CourseSearchActivity.class);
                    intent.putExtra("query", query);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
            // 触摸即跳转：拦截触摸，防止获取焦点与弹出键盘
            et_search_course.setOnTouchListener((v, event) -> {
                if (event != null && event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        openCourseSearchFragment();
                    } catch (Exception e) {
                        Log.e("MainActivity", "open CourseSearchFragment failed: " + e.getMessage());
                    }
                    return true; // 消耗事件，避免弹出键盘
                }
                return false;
            });
            // 移除右侧按钮点击逻辑：保留键盘搜索/回车触发
        }
        initBodyLayout();
    }

    /**
     * 获取底部导航栏上的控件
     */
    private void initBottomBar() {
        mBottomLayout = (LinearLayout) findViewById(R.id.main_bottom_bar);
        mCourseBtn = findViewById(R.id.bottom_bar_course_btn);
        mExercisesBtn = findViewById(R.id.bottom_bar_exercises_btn);
        mMyInfoBtn = findViewById(R.id.bottom_bar_myinfo_btn);
        mCommunityBtn = findViewById(R.id.bottom_bar_community_btn);
        tv_course = (TextView) findViewById(R.id.bottom_bar_text_course);
        tv_exercises = (TextView) findViewById(R.id.bottom_bar_text_exercises);
        tv_myInfo = (TextView) findViewById(R.id.bottom_bar_text_myinfo);
        tv_community = (TextView) findViewById(R.id.bottom_bar_text_community);
        iv_course = (ImageView) findViewById(R.id.bottom_bar_image_course);
        iv_exercises = (ImageView) findViewById(R.id.bottom_bar_image_exercises);
        iv_myInfo = (ImageView) findViewById(R.id.bottom_bar_image_myinfo);
        iv_community = (ImageView) findViewById(R.id.bottom_bar_image_community);
        mArticleBtn = findViewById(R.id.bottom_bar_article_btn);
        mAiBtn = findViewById(R.id.bottom_bar_ai_btn);
        tv_article = (TextView) findViewById(R.id.bottom_bar_text_article);
        tv_ai = (TextView) findViewById(R.id.bottom_bar_text_ai);
        iv_article = (ImageView) findViewById(R.id.bottom_bar_image_article);
        iv_ai = (ImageView) findViewById(R.id.bottom_bar_image_ai);
    }

    private void initBodyLayout() {
        mBodyLayout = (FrameLayout) findViewById(R.id.main_body);
    }

    /**
     * 获取主内容容器
     */
    public FrameLayout getBodyLayout() {
        return mBodyLayout;
    }

    /**
     * 控件的点击事件
     */
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.bottom_bar_course_btn) {
            clearBottomImageState();
            selectDisplayView(0);
        } else if (viewId == R.id.bottom_bar_exercises_btn) {
            clearBottomImageState();
            selectDisplayView(1);
        } else if (viewId == R.id.bottom_bar_community_btn) {
            clearBottomImageState();
            selectDisplayView(5);
        } else if (viewId == R.id.bottom_bar_myinfo_btn) {
            clearBottomImageState();
            selectDisplayView(2);
            if (mMyInfoView != null) {
                mMyInfoView.setLoginParams(readLoginStatus());
            }
        } else if (viewId == R.id.bottom_bar_article_btn) {
            clearBottomImageState();
            selectDisplayView(3);
        } else if (viewId == R.id.bottom_bar_ai_btn) {
            clearBottomImageState();
            selectDisplayView(4);
        }
    }

    /**
     * 设置底部三个按钮的点击监听事件
     */
    private void setListener() {
        for (int i = 0; i < mBottomLayout.getChildCount(); i++) {
            mBottomLayout.getChildAt(i).setOnClickListener(this);
        }
        // 顶部播放历史点击
        if (ll_play_history_top != null) {
            ll_play_history_top.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isLogin = readLoginStatus();
                    if (isLogin) {
                        Intent intent = new Intent(MainActivity.this, com.example.xinqiao.activity.PlayHistoryActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.login_required), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    /**
     * 清除底部按钮的选中状态
     */
    private void clearBottomImageState() {
        tv_course.setTextColor(getResources().getColor(R.color.bottom_nav_unselected_healing));
        tv_exercises.setTextColor(getResources().getColor(R.color.bottom_nav_unselected_healing));
        tv_myInfo.setTextColor(getResources().getColor(R.color.bottom_nav_unselected_healing));
        tv_article.setTextColor(getResources().getColor(R.color.bottom_nav_unselected_healing));
        tv_ai.setTextColor(getResources().getColor(R.color.bottom_nav_unselected_healing));
        if (tv_community != null) {
            tv_community.setTextColor(getResources().getColor(R.color.bottom_nav_unselected_healing));
        }
        iv_course.setImageResource(R.drawable.main_course_icon);
        iv_exercises.setImageResource(R.drawable.main_exercises_icon);
        iv_article.setImageResource(R.drawable.main_article_icon);
        iv_ai.setImageResource(R.drawable.main_ai_icon);
        iv_myInfo.setImageResource(R.drawable.main_my_icon);
        if (iv_community != null) {
            iv_community.setImageResource(R.mipmap.main_community_icon);
            iv_community.clearColorFilter();
        }
    }

    /**
     * 设置底部按钮选中状态
     */
    public void setSelectedStatus(int index) {
        clearBottomImageState();
        switch (index) {
            case 0:
                iv_course.setImageResource(R.drawable.main_course_icon);
                tv_course.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.GONE);
                tv_main_title.setText(getString(R.string.psych_courses));
                // 课程页搜索与播放历史功能移到自定义标题栏
                if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
                if (tv_main_title != null) tv_main_title.setVisibility(View.GONE);
                break;
            case 1:
                iv_exercises.setImageResource(R.drawable.main_exercises_icon);
                tv_exercises.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.GONE); // 习题页面隐藏顶部栏
                // 清理显示状态
                if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
                if (tv_main_title != null) tv_main_title.setVisibility(View.VISIBLE);
                break;
            case 5:
                if (iv_community != null) {
                    iv_community.setImageResource(R.mipmap.main_community_icon);
                    // 使用颜色滤镜突出选中态，避免资源缺失导致无选中外观
                    iv_community.setColorFilter(getResources().getColor(R.color.bottom_nav_selected_healing), PorterDuff.Mode.SRC_IN);
                }
                if (tv_community != null) {
                    tv_community.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                }
                // 社区页面隐藏全局标题栏，使用页面内部头部
                rl_title_bar.setVisibility(View.GONE);
                if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
                if (tv_main_title != null) tv_main_title.setVisibility(View.GONE);
                break;
            case 2:
                iv_myInfo.setImageResource(R.drawable.main_my_icon);
                tv_myInfo.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.GONE);
                if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
                if (tv_main_title != null) tv_main_title.setVisibility(View.VISIBLE);
                break;
            case 3:
                iv_article.setImageResource(R.drawable.main_article_icon);
                tv_article.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                // 文章页顶部应为自身搜索框与标签栏，隐藏全局标题栏
                rl_title_bar.setVisibility(View.GONE);
                if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
                if (tv_main_title != null) tv_main_title.setVisibility(View.GONE);
                break;
            case 4:
                iv_ai.setImageResource(R.drawable.main_ai_icon);
                tv_ai.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.GONE);
                if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
                if (tv_main_title != null) tv_main_title.setVisibility(View.VISIBLE);
                break;
            
        }
    }

    /**
     * 移除不需要的视图
     */
    private void removeAllView() {
        try {
            if (mBodyLayout != null) {
                mBodyLayout.post(() -> {
                    try {
                        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                        
                        // 显示底部导航栏（当清除所有Fragment时）
                        if (mBottomLayout != null) {
                            mBottomLayout.setVisibility(View.VISIBLE);
                        }
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        if (fragments != null && !fragments.isEmpty()) {
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            for (androidx.fragment.app.Fragment fragment : fragments) {
                                if (fragment != null) {
                                    transaction.remove(fragment);
                                }
                            }
                            transaction.commitAllowingStateLoss();
                        }
                        mBodyLayout.removeAllViews();
                    } catch (Exception ex) {
                        Log.e("MainActivity", "Error removing views: " + ex.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error removing views: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置界面view的初始化状态
     */
    private void setInitStatus() {
        clearBottomImageState();
        setSelectedStatus(0);
        createView(0);
        // 其他视图按需创建，避免启动时阻塞主线程

        // 初始化当前索引与返回栈
        currentIndex = 0;
        viewHistory.clear();
    }

    /**
     * 显示对应的页面
     */
    private void selectDisplayView(int index) {
        if (mBodyLayout != null) {
            mBodyLayout.post(() -> selectDisplayViewInternal(index, true));
        } else {
            selectDisplayViewInternal(index, true);
        }
    }

    // 支持选择视图时是否记录历史，用于“返回到上一个界面”的行为
    private void selectDisplayViewInternal(int index, boolean recordHistory) {
        try {
            if (recordHistory && currentIndex != -1 && index != currentIndex) {
                viewHistory.push(currentIndex);
            }
            // 先隐藏所有视图
            hideAllViews();

            // 创建并显示对应视图
            createView(index);
            setSelectedStatus(index);
            currentIndex = index;
        } catch (Exception e) {
            Log.e("MainActivity", "Error selecting display view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 隐藏所有视图
     */
    private void hideAllViews() {
        try {
            // 改为隐藏视图而不是移除它们
            if (mCourseView != null && mCourseView.getView() != null) {
                mCourseView.hideView();
            }
            if (mExercisesView != null && mExercisesView.getView() != null) {
                mExercisesView.getView().setVisibility(View.GONE);
            }
            if (mMyInfoView != null && mMyInfoView.getView() != null) {
                mMyInfoView.getView().setVisibility(View.GONE);
            }
            if (mArticleView != null && mArticleView.getView() != null) {
                mArticleView.setVisibility(View.GONE);
            }
            if (mConsultationView != null && mConsultationView.getView() != null) {
                mConsultationView.hideView();
            }
            if (mCommunityView != null && mCommunityView.getView() != null) {
                mCommunityView.hideView();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error hiding views: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 选择视图
     */
    private void createView(int viewIndex) {
        try {
            switch (viewIndex) {
                case 0:
                    //课程界面
                    if (mCourseView == null) {
                        mCourseView = new CourseView(this);
                    }
                    // 检查视图是否已添加到布局中
                    if (mCourseView.getView().getParent() == null) {
                        mBodyLayout.addView(mCourseView.getView());
                    }
                    mCourseView.showView();
                    break;
                case 1:
                    //习题界面
                    if (mExercisesView == null) {
                        mExercisesView = new ExercisesView(this);
                    }
                    // 检查视图是否已添加到布局中
                    if (mExercisesView.getView().getParent() == null) {
                        mBodyLayout.addView(mExercisesView.getView());
                    }
                    mExercisesView.showView();
                    break;
                case 2:
                    //我的界面
                    if (mMyInfoView == null) {
                        mMyInfoView = new MyInfoView(this);
                    }
                    // 检查视图是否已添加到布局中
                    if (mMyInfoView.getView().getParent() == null) {
                        mBodyLayout.addView(mMyInfoView.getView());
                    }
                    mMyInfoView.showView();
                    break;
                case 3:
                    // 文章界面
                    if (mArticleView == null) {
                        mArticleView = new ArticleView(this);
                    }
                    // 检查视图是否已添加到布局中
                    if (mArticleView.getView().getParent() == null) {
                        mBodyLayout.addView(mArticleView.getView());
                    }
                    mArticleView.showView();
                    break;
                case 4:
                    // 咨询界面（聚合专业咨询+AI浮窗）
                    if (mConsultationView == null) {
                        mConsultationView = new ConsultationView(this);
                    }
                    // 检查视图是否已添加到布局中
                    if (mConsultationView.getView().getParent() == null) {
                        mBodyLayout.addView(mConsultationView.getView());
                    }
                    mConsultationView.showView();
                    break;
                case 5:
                    // 社区界面（Compose）
                    if (mCommunityView == null) {
                        mCommunityView = new CommunityView(this);
                    }
                    if (mCommunityView.getView().getParent() == null) {
                        mBodyLayout.addView(mCommunityView.getView());
                    }
                    mCommunityView.showView();
                    break;
                
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error creating view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            boolean isLogin = data.getBooleanExtra("isLogin", false);
            boolean avatarUpdated = data.getBooleanExtra("avatar_updated", false);
            if (isLogin) { //登录成功时显示课程界面
                clearBottomImageState();
                selectDisplayView(0);
            }
            if (mMyInfoView != null && (isLogin || avatarUpdated)) { //登录成功、退出登录或头像更换时刷新"我"页面
                mMyInfoView.setLoginParams(true);
            }
        }
    }

    protected long exitTime; //记录第一次点击时的时间

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            // 优先让社区页面内部处理返回（例如从详情回到列表）
            try {
                if (mCommunityView != null && mCommunityView.getView() != null && mCommunityView.getView().getVisibility() == View.VISIBLE) {
                    boolean consumed = false;
                    try { consumed = mCommunityView.handleBackPressed(); } catch (Exception ignore) {}
                    if (consumed) {
                        setSelectedStatus(5);
                        return true;
                    }
                }
            } catch (Exception ignore) {}
            // 先检查是否有Fragment在返回栈中
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                if (mBodyLayout != null) {
                    mBodyLayout.post(() -> {
                        try {
                            getSupportFragmentManager().popBackStack();
                            
                            // 显示底部导航栏（当从Fragment返回时）
                            if (mBottomLayout != null) {
                                mBottomLayout.setVisibility(View.VISIBLE);
                            }
                            
                            if (mExercisesView != null && mExercisesView.getView().getVisibility() == View.VISIBLE) {
                                setSelectedStatus(1);
                            } else if (mCourseView != null && mCourseView.getView().getVisibility() == View.VISIBLE) {
                                setSelectedStatus(0);
                            } else if (mMyInfoView != null && mMyInfoView.getView().getVisibility() == View.VISIBLE) {
                                setSelectedStatus(2);
                            } else if (mArticleView != null && mArticleView.getVisibility() == View.VISIBLE) {
                                setSelectedStatus(3);
                            } else if (mConsultationView != null && mConsultationView.getView() != null && mConsultationView.getView().getVisibility() == View.VISIBLE) {
                                setSelectedStatus(4);
                            } else if (mCommunityView != null && mCommunityView.getView() != null && mCommunityView.getView().getVisibility() == View.VISIBLE) {
                                setSelectedStatus(5);
                            } else {
                                if (!viewHistory.isEmpty()) {
                                    int prev = viewHistory.pop();
                                    selectDisplayViewInternal(prev, false);
                                } else {
                                    selectDisplayViewInternal(0, false);
                                }
                            }
                        } catch (Exception ex) {
                            Log.e("MainActivity", "Back stack pop error: " + ex.getMessage());
                        }
                    });
                }
                return true;
            }
            
            // 没有Fragment：优先使用视图返回栈回到上一个界面
            if (!viewHistory.isEmpty()) {
                int prev = viewHistory.pop();
                selectDisplayViewInternal(prev, false);
            } else {
                // 返回栈为空时，执行双击退出逻辑
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    Toast.makeText(MainActivity.this, getString(R.string.exit_confirm),
                            Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                } else {
                    // 使用 finishAffinity() 安全结束任务栈，保留登录信息
                    MainActivity.this.finishAffinity();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 获取SharedPreferences中的登录状态
     */
    private boolean readLoginStatus() {
        SharedPreferences sp = getSharedPreferences("loginInfo",
                Context.MODE_PRIVATE);
        boolean isLogin = sp.getBoolean("isLogin", false);
        return isLogin;
    }

    /**
     * 清除SharedPreferences中的登录状态
     */
    private void clearLoginStatus() {
        SharedPreferences sp = getSharedPreferences("loginInfo",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit(); //获取编辑器
        editor.putBoolean("isLogin", false); //清除登录状态
        editor.putString("loginUserName", ""); //清除登录时的用户名
        editor.commit(); //提交修改
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 销毁视图组件
        if (mCourseView != null) {
            mCourseView.onDestroy();
            mCourseView = null;
        }
        
        if (mExercisesView != null) {
            mExercisesView = null;
        }
        
        if (mMyInfoView != null) {
            mMyInfoView = null;
        }
        
        if (mArticleView != null) {
            mArticleView = null;
        }
        
        if (mConsultationView != null) {
            mConsultationView = null;
        }
        
        // 清理Fragment
        try {
            if (mBodyLayout != null) {
                mBodyLayout.post(() -> {
                    try {
                        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                    } catch (Exception ex) {
                        Log.e("MainActivity", "Error clearing fragments in onDestroy: " + ex.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error clearing fragments in onDestroy: " + e.getMessage());
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停课程视图的自动轮播
        if (mCourseView != null) {
            mCourseView.hideView();
        }
        
        // 暂停其他视图的活动 - 设置为不可见
        if (mExercisesView != null && mExercisesView.getView() != null) {
            mExercisesView.getView().setVisibility(View.GONE);
        }
        
        if (mArticleView != null) {
            mArticleView.setVisibility(View.GONE);
        }
        
        if (mConsultationView != null && mConsultationView.getView() != null) {
            mConsultationView.hideView();
        }

        if (mMyInfoView != null && mMyInfoView.getView() != null) {
            mMyInfoView.getView().setVisibility(View.GONE);
        }

        if (mCommunityView != null && mCommunityView.getView() != null) {
            mCommunityView.hideView();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences spRole = getSharedPreferences("loginInfo", Context.MODE_PRIVATE);
        boolean isCounselorRole = spRole.getBoolean("isCounselor", false);
        if (isCounselorRole) {
            try {
                Intent intent = new Intent(MainActivity.this, com.example.xinqiao.activity.CounselorMainActivity.class);
                startActivity(intent);
                MainActivity.this.finishAffinity();
            } catch (Exception ignored) {}
            return;
        }
        // 检查是否有视图可见，如果没有则恢复到上一次显示的视图
        boolean hasVisibleView = false;
        
        if (mCourseView != null && mCourseView.getView().getVisibility() == View.VISIBLE) {
            mCourseView.showView();
            hasVisibleView = true;
        }
        
        if (mExercisesView != null && mExercisesView.getView() != null && mExercisesView.getView().getVisibility() == View.VISIBLE) {
            mExercisesView.showView();
            hasVisibleView = true;
        }
        
        if (mArticleView != null && mArticleView.getView() != null && mArticleView.getView().getVisibility() == View.VISIBLE) {
            mArticleView.showView();
            hasVisibleView = true;
        }
        
        if (mConsultationView != null && mConsultationView.getView() != null && mConsultationView.getView().getVisibility() == View.VISIBLE) {
            mConsultationView.showView();
            hasVisibleView = true;
        }
        
        if (mMyInfoView != null && mMyInfoView.getView() != null && mMyInfoView.getView().getVisibility() == View.VISIBLE) {
            mMyInfoView.showView();
            hasVisibleView = true;
        }

        if (mCommunityView != null && mCommunityView.getView() != null && mCommunityView.getView().getVisibility() == View.VISIBLE) {
            mCommunityView.showView();
            hasVisibleView = true;
        }
        
        // 如果没有任何视图可见，则恢复到当前索引对应的视图；若未初始化则显示课程视图
        if (!hasVisibleView) {
            int targetIndex = (currentIndex != -1) ? currentIndex : 0;
            selectDisplayViewInternal(targetIndex, false);
        }

        // 额外兜底：当容器中没有任何子视图（例如语言切换后返回导致视图销毁），强制初始化默认视图
        try {
            if (mBodyLayout != null && mBodyLayout.getChildCount() == 0) {
                setInitStatus();
            }
            // 若所有子视图均为 GONE，且当前索引未能恢复，主动显示课程视图
            boolean allGone = true;
            try {
                allGone = (mCourseView == null || mCourseView.getView() == null || mCourseView.getView().getVisibility() != View.VISIBLE)
                        && (mExercisesView == null || mExercisesView.getView() == null || mExercisesView.getView().getVisibility() != View.VISIBLE)
                        && (mMyInfoView == null || mMyInfoView.getView() == null || mMyInfoView.getView().getVisibility() != View.VISIBLE)
                        && (mArticleView == null || mArticleView.getView() == null || mArticleView.getView().getVisibility() != View.VISIBLE)
                        && (mConsultationView == null || mConsultationView.getView() == null || mConsultationView.getView().getVisibility() != View.VISIBLE)
                        && (mCommunityView == null || mCommunityView.getView() == null || mCommunityView.getView().getVisibility() != View.VISIBLE);
            } catch (Exception ignore2) {}
            if (allGone) {
                try {
                    createView(0);
                    setSelectedStatus(0);
                    currentIndex = 0;
                } catch (Exception ignore3) {}
            }
        } catch (Exception ignore) {}

        // 若当前顶部是课程搜索Fragment，则确保标题栏与课程页特有控件保持隐藏，避免返回后出现双搜索框
        try {
            Fragment f = getSupportFragmentManager().findFragmentByTag("CourseSearchFragment");
            if (f != null && f.isVisible()) {
                if (rl_title_bar != null) rl_title_bar.setVisibility(View.GONE);
                if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
            } else {
                // 当搜索 Fragment 已关闭时，根据当前页面状态恢复标题栏
                // 若课程页可见或当前索引为课程页，则显示课程搜索框与播放历史
                boolean courseVisible = false;
                try {
                    courseVisible = (mCourseView != null && mCourseView.getView() != null
                            && mCourseView.getView().getVisibility() == View.VISIBLE);
                } catch (Exception ignore2) {}

                if (courseVisible || currentIndex == 0) {
                    if (rl_title_bar != null) rl_title_bar.setVisibility(View.GONE);
                    if (et_search_course != null) et_search_course.setVisibility(View.GONE);
                    if (ll_play_history_top != null) ll_play_history_top.setVisibility(View.GONE);
                    if (tv_main_title != null) tv_main_title.setVisibility(View.GONE);
                }
            }
        } catch (Exception ignore) {}
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            if (mBodyLayout != null && mBodyLayout.getChildCount() == 0) {
                setInitStatus();
                return;
            }
            int targetIndex = (currentIndex != -1) ? currentIndex : 0;
            selectDisplayViewInternal(targetIndex, false);
        } catch (Exception ignore) {}
    }

    /**
     * 打开课程搜索 Fragment 页面
     */
    public void openCourseSearchFragment() {
        try {
            if (mBodyLayout != null) {
                mBodyLayout.post(() -> {
                    try {
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        CourseSearchFragment fragment = new CourseSearchFragment();
                        transaction.replace(getBodyLayout().getId(), fragment, "CourseSearchFragment");
                        transaction.addToBackStack(null);
                        transaction.commitAllowingStateLoss();
                    } catch (Exception ex) {
                        Log.e("MainActivity", "openCourseSearchFragment error: " + ex.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "openCourseSearchFragment error: " + e.getMessage());
        }
    }
}
