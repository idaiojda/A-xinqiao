package com.example.xinqiao.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.xinqiao.R;
import com.example.xinqiao.view.ArticleView;
import com.example.xinqiao.view.CourseView;
import com.example.xinqiao.view.ExercisesView;
import com.example.xinqiao.view.MyInfoView;
import com.example.xinqiao.view.ConsultationView;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.mysql.DBUtils;

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
    private TextView tv_course;
    private TextView tv_exercises;
    private TextView tv_myInfo;
    private TextView tv_article;
    private TextView tv_ai;
    private ImageView iv_course;
    private ImageView iv_exercises;
    private ImageView iv_myInfo;
    private ImageView iv_article;
    private ImageView iv_ai;
    private TextView tv_back;
    private TextView tv_main_title;
    private RelativeLayout rl_title_bar;
    // 用于后台任务的线程池
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //设置此界面为竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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
            // 延迟初始化默认视图，避免阻塞主线程
            mBodyLayout.postDelayed(() -> setInitStatus(), 100);
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
        // 延迟恢复视图状态，避免阻塞主线程
        mBodyLayout.post(() -> selectDisplayView(currentView));
    }

    /**
     * 获取界面上的UI控件
     */
    private void init() {
        tv_back = (TextView) findViewById(R.id.tv_back);
        tv_main_title = (TextView) findViewById(R.id.tv_main_title);
        tv_main_title.setText("心理课程");
        rl_title_bar = (RelativeLayout) findViewById(R.id.title_bar);
        tv_back.setVisibility(View.GONE);
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
        tv_course = (TextView) findViewById(R.id.bottom_bar_text_course);
        tv_exercises = (TextView) findViewById(R.id.bottom_bar_text_exercises);
        tv_myInfo = (TextView) findViewById(R.id.bottom_bar_text_myinfo);
        iv_course = (ImageView) findViewById(R.id.bottom_bar_image_course);
        iv_exercises = (ImageView) findViewById(R.id.bottom_bar_image_exercises);
        iv_myInfo = (ImageView) findViewById(R.id.bottom_bar_image_myinfo);
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
        iv_course.setImageResource(R.drawable.main_course_icon);
        iv_exercises.setImageResource(R.drawable.main_exercises_icon);
        iv_article.setImageResource(R.drawable.main_article_icon);
        iv_ai.setImageResource(R.drawable.main_ai_icon);
        iv_myInfo.setImageResource(R.drawable.main_my_icon);
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
                rl_title_bar.setVisibility(View.VISIBLE);
                tv_main_title.setText("心理课程");
                break;
            case 1:
                iv_exercises.setImageResource(R.drawable.main_exercises_icon);
                tv_exercises.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.GONE); // 习题页面隐藏顶部栏
                break;
            case 2:
                iv_myInfo.setImageResource(R.drawable.main_my_icon);
                tv_myInfo.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.GONE);
                break;
            case 3:
                iv_article.setImageResource(R.drawable.main_article_icon);
                tv_article.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.VISIBLE);
                tv_main_title.setText("心理文章");
                break;
            case 4:
                iv_ai.setImageResource(R.drawable.main_ai_icon);
                tv_ai.setTextColor(getResources().getColor(R.color.bottom_nav_selected_healing));
                rl_title_bar.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * 移除不需要的视图
     */
    private void removeAllView() {
        try {
            // 先清理所有Fragment
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            
            // 移除所有可能存在的Fragment
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            if (fragments != null && !fragments.isEmpty()) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                for (androidx.fragment.app.Fragment fragment : fragments) {
                    if (fragment != null) {
                        transaction.remove(fragment);
                    }
                }
                transaction.commitNow();
            }
            
            // 清空视图容器
            mBodyLayout.removeAllViews();
            
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
        // 确保其他视图也被初始化但隐藏
        createView(1);
        createView(2);
        createView(3);
        createView(4);
        
        // 隐藏除课程页面外的所有视图
        if (mExercisesView != null) mExercisesView.getView().setVisibility(View.GONE);
        if (mMyInfoView != null) mMyInfoView.getView().setVisibility(View.GONE);
        if (mArticleView != null) mArticleView.setVisibility(View.GONE);
        if (mConsultationView != null) mConsultationView.hideView();
    }

    /**
     * 显示对应的页面
     */
    private void selectDisplayView(int index) {
        try {
            // 先隐藏所有视图
            hideAllViews();
            
            // 创建并显示对应视图
            createView(index);
            setSelectedStatus(index);
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
            // 先检查是否有Fragment在返回栈中
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                // 有Fragment在返回栈中，先处理Fragment的返回
                getSupportFragmentManager().popBackStackImmediate();
                
                // 检查当前显示的视图，并更新底部导航栏状态
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
                } else {
                    // 如果没有视图可见，显示默认的课程视图
                    selectDisplayView(0);
                }
                return true;
            }
            
            // 没有Fragment，处理应用退出逻辑
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(MainActivity.this, "再按一次退出心桥",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                // 使用 finishAffinity() 安全结束任务栈，保留登录信息
                MainActivity.this.finishAffinity();
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
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 检查是否有视图可见，如果没有则显示默认课程视图
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
        
        // 如果没有任何视图可见，显示默认课程视图
        if (!hasVisibleView) {
            selectDisplayView(0);
        }
    }
}