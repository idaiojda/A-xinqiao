package com.example.xinqiao.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.PlayHistoryActivity;
import com.example.xinqiao.adapter.AdBannerAdapter;
// import com.example.xinqiao.adapter.CourseAdapter; // replaced by RecyclerView adapter
import com.example.xinqiao.adapter.CourseListAdapter;
import com.example.xinqiao.util.ui.RecyclerViewOptimizer;
import com.example.xinqiao.util.ui.SpacesItemDecoration;
import com.example.xinqiao.bean.CourseBean;
import com.example.xinqiao.util.AnalysisUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CourseView {
    // 根视图
    private View mRootView;
    // 课程列表RecyclerView
    private RecyclerView rv_list;
    // 课程适配器（RecyclerView + ListAdapter）
    private CourseListAdapter adapter;
    // 课程数据列表
    private List<List<CourseBean>> cbl;
    // 上下文
    private FragmentActivity mContext;
    // 布局加载器
    private LayoutInflater mInflater;
    // 当前视图
    private View mCurrentView;
    // 广告轮播ViewPager
    private ViewPager adPager;// 广告
    // 广告条容器
    private View adBannerLay;// 广告条容器
    // 广告适配器
    private AdBannerAdapter ada;// 适配器
    // 广告自动滑动消息常量
    public static final int MSG_AD_SLID = 002;// 广告自动滑动
    // 小圆点指示器
    private ViewPagerIndicator vpi;// 小圆点
    // 事件捕获Handler
    private MHandler mHandler;// 事件捕获
    // 广告数据列表
    private List<CourseBean> cadl;
    // 控制广告自动轮播的标志位
    private AtomicBoolean isAutoSliding = new AtomicBoolean(true);

    /**
     * 构造方法，初始化上下文和布局加载器
     */
    public CourseView(FragmentActivity context) {
        mContext = context;
        // 为之后将Layout转化为view时用
        mInflater = LayoutInflater.from(mContext);
    }

    /**
     * 创建视图，初始化广告数据、课程数据、控件，并启动广告自动轮播线程
     */
    private void createView() {
        mHandler = new MHandler(); // 初始化Handler
        initAdData();              // 初始化广告数据
        initView();                // 初始化控件（先设置空数据的适配器）
        loadCourseDataAsync();     // 异步加载课程数据，完成后刷新适配器
        startAdAutoSlide();        // 启动广告自动轮播
    }

    /**
     * 事件捕获Handler，处理广告自动滑动消息
     */
    class MHandler extends Handler {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case MSG_AD_SLID:
                    // 收到自动滑动消息后，切换到下一页
                    if (isAutoSliding.get() && ada != null && ada.getCount() > 0 && adPager != null) {
                        adPager.setCurrentItem(adPager.getCurrentItem() + 1);
                    }
                    break;
            }
        }
    }

    /**
     * 启动广告自动轮播
     */
    private void startAdAutoSlide() {
        // 使用postDelayed实现更精确的定时控制
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAutoSliding.get()) {
                        mHandler.sendEmptyMessage(MSG_AD_SLID);
                        // 继续下一次轮播
                        mHandler.postDelayed(this, 5000);
                    }
                }
            }, 5000);
        }
    }

    /**
     * 初始化控件，包括课程列表、广告轮播、指示器等
     */
    private void initView() {
        mCurrentView = mInflater.inflate(R.layout.main_view_course, null);
        // 初始化课程 RecyclerView
        rv_list = (RecyclerView) mCurrentView.findViewById(R.id.rv_list);
        if (rv_list != null) {
            // 瀑布流布局，两列
            StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
            rv_list.setLayoutManager(sglm);
            // 间距装饰：8dp，含边距
            int spacingPx = dpToPx(8);
            rv_list.addItemDecoration(new SpacesItemDecoration(spacingPx, true));
            // Padding 与裁剪设置
            rv_list.setClipToPadding(false);
            rv_list.setPadding(spacingPx, spacingPx, spacingPx, spacingPx);
            // 性能优化
            RecyclerViewOptimizer.optimizeDefault(rv_list);
            // 适配器
            adapter = new CourseListAdapter(mContext);
            rv_list.setAdapter(adapter);
            // 先提交空数据，避免主线程阻塞
            adapter.submitList(new ArrayList<com.example.xinqiao.bean.CourseBean>());
        }
        
        // 初始化自定义标题栏的搜索功能
        View searchContainer = mCurrentView.findViewById(R.id.ll_search_container);
        if (searchContainer != null) {
            searchContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        openCourseSearchFragment();
                    } catch (Exception e) {
                        android.util.Log.e("CourseView", "打开课程搜索失败: " + e.getMessage());
                    }
                }
            });
        }
        View etSearchTop = mCurrentView.findViewById(R.id.et_search_top);
        if (etSearchTop != null) {
            etSearchTop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        openCourseSearchFragment();
                    } catch (Exception e) {
                        android.util.Log.e("CourseView", "打开课程搜索失败: " + e.getMessage());
                    }
                }
            });
        }
        
        // 初始化自定义标题栏的播放历史功能
        View playHistoryBtn = mCurrentView.findViewById(R.id.ll_play_history_custom);
        if (playHistoryBtn != null) {
            playHistoryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.util.Log.d("CourseView", "播放历史按钮被点击");
                    // 检查用户是否登录
                    boolean isLogin = readLoginStatus();
                    android.util.Log.d("CourseView", "用户登录状态: " + isLogin);
                    if (isLogin) {
                        // 跳转到播放历史页面
                        android.util.Log.d("CourseView", "准备跳转到播放历史页面");
                        Intent intent = new Intent(mContext, PlayHistoryActivity.class);
                        mContext.startActivity(intent);
                    } else {
                        Toast.makeText(mContext, "您还未登录，请先登录", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
        // 初始化广告轮播ViewPager
        adPager = (ViewPager) mCurrentView.findViewById(R.id.vp_advertBanner);
        if (adPager != null) {
            adPager.setLongClickable(false);
            ada = new AdBannerAdapter(mContext.getSupportFragmentManager(),
                    mHandler);
            adPager.setAdapter(ada);// 给ViewPager设置适配器
            adPager.setOnTouchListener(ada); // 设置触摸监听，滑动时暂停自动轮播
            
            // 设置初始位置为中间位置，确保可以向两个方向滑动
            adPager.setCurrentItem(ada.getCount() / 2);
            
            // 初始化小圆点指示器
            vpi = (ViewPagerIndicator) mCurrentView
                    .findViewById(R.id.vpi_advert_indicator);// 获取广告条上的小圆点
            if (vpi != null && ada.getSize() > 0) {
                vpi.setCount(ada.getSize());// 设置小圆点的个数
                vpi.setCurrentPosition(0);
            }
            adBannerLay = mCurrentView.findViewById(R.id.rl_adBanner);
            // 监听ViewPager页面切换，更新小圆点位置
            adPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }
                @Override
                public void onPageSelected(int position) {
                    if (ada != null && ada.getSize() > 0 && vpi != null) {
                        // 由于index数据在滑动时是累加的，因此用index % ada.getSize()来标记滑动到的当前位置
                        vpi.setCurrentPosition(position % ada.getSize());
                    }
                }
                @Override
                public void onPageScrollStateChanged(int state) {
                    // 当用户手动滑动时，暂停自动轮播
                    if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                        isAutoSliding.set(false);
                    } else if (state == ViewPager.SCROLL_STATE_IDLE) {
                        // 滑动结束后恢复自动轮播
                        isAutoSliding.set(true);
                    }
                }
            });
            // 重置广告条大小
            resetSize();
            // 设置广告数据和小圆点初始状态
            if (cadl != null && ada != null) {
                if (cadl.size() > 0 && vpi != null) {
                    vpi.setCount(cadl.size());
                    vpi.setCurrentPosition(0);
                }
                ada.setDatas(cadl);
            }
        }
    }

    /**
     * 异步加载课程数据，解析完成后在主线程刷新适配器
     */
    private void loadCourseDataAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<List<CourseBean>> result = new ArrayList<>();
                String errorMessage = null;
                
                try {
                    android.util.Log.d("CourseView", "开始从后端API加载课程数据...");
                    // 使用阻塞方式调用Kotlin suspend函数
                    java.util.List<com.example.xinqiao.counselor.CourseDto> courses = 
                        kotlinx.coroutines.BuildersKt.runBlocking(
                            kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
                            (coroutineScope, continuation) -> com.example.xinqiao.util.CourseLoader.INSTANCE.loadCourses(continuation)
                        );
                    
                    if (courses != null && !courses.isEmpty()) {
                        android.util.Log.d("CourseView", "成功加载 " + courses.size() + " 个课程");
                        // 将CourseDto转换为CourseBean
                        List<CourseBean> courseBeans = new ArrayList<>();
                        for (com.example.xinqiao.counselor.CourseDto dto : courses) {
                            CourseBean bean = new CourseBean();
                            bean.id = (int) dto.getId();
                            bean.title = dto.getTitle();
                            bean.imgTitle = dto.getTitle();
                            bean.intro = dto.getDescription() != null ? dto.getDescription() : "";
                            bean.icon = dto.getCoverImage();
                            bean.coverImage = dto.getCoverImage(); // 添加coverImage字段
                            bean.lessonCount = dto.getLessonCount();
                            bean.premium = dto.getPremium();
                            bean.price = dto.getPrice();
                            courseBeans.add(bean);
                        }
                        // 将所有课程放在一个组中
                        if (!courseBeans.isEmpty()) {
                            result.add(courseBeans);
                        }
                    } else {
                        android.util.Log.w("CourseView", "后端返回的课程列表为空");
                        errorMessage = "暂无课程数据";
                    }
                } catch (Exception e) {
                    android.util.Log.e("CourseView", "加载课程失败: " + e.getMessage(), e);
                    errorMessage = "无法连接到服务器，请检查网络连接";
                }
                
                final List<List<CourseBean>> finalResult = result;
                final String finalErrorMessage = errorMessage;
                
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cbl = (finalResult != null && !finalResult.isEmpty()) ? finalResult : new ArrayList<List<CourseBean>>();
                        if (adapter != null) {
                            List<CourseBean> flat = flattenCourseData(cbl);
                            adapter.submitList(flat);
                        }
                        
                        // 如果加载失败，显示错误提示
                        if (finalErrorMessage != null) {
                            Toast.makeText(mContext, finalErrorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 将二维课程数据拍平为一维列表，供 RecyclerView 使用
     */
    private List<CourseBean> flattenCourseData(List<List<CourseBean>> groups) {
        List<CourseBean> out = new ArrayList<CourseBean>();
        if (groups != null) {
            for (List<CourseBean> g : groups) {
                if (g != null) {
                    out.addAll(g);
                }
            }
        }
        return out;
    }

    private int dpToPx(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * 计算广告条控件的大小，使其宽度为屏幕宽度，高度为宽度一半
     */
    private void resetSize() {
        if (adBannerLay != null) {
            int sw = getScreenWidth(mContext);
            int adLheight = sw / 2;// 广告条高度
            ViewGroup.LayoutParams adlp = adBannerLay.getLayoutParams();
            adlp.width = sw;
            adlp.height = adLheight;
            adBannerLay.setLayoutParams(adlp);
        }
    }

    /**
     * 获取屏幕宽度
     */
    public static int getScreenWidth(Activity context) {
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = context.getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        return metrics.widthPixels;
    }

    /**
     * 初始化广告数据，构造3个广告Banner
     */
    private void initAdData() {
        cadl = new ArrayList<CourseBean>();
        for (int i = 0; i < 3; i++) {
            CourseBean bean = new CourseBean();
            bean.id=(i + 1);
            switch (i) {
                case 0:
                    bean.icon="banner_1";
                    break;
                case 1:
                    bean.icon="banner_2";
                    break;
                case 2:
                    bean.icon="banner_3";
                    break;
                default:
                    break;
            }
            cadl.add(bean);
        }
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
        // 恢复自动轮播
        isAutoSliding.set(true);
        // 重新设置适配器以刷新轮播图
        if (ada != null && cadl != null) {
            ada.setDatas(cadl);
            // 刷新ViewPager以确保图片正确显示
            if (adPager != null) {
                adPager.setAdapter(ada);
            }
        }
    }
    
    /**
     * 隐藏视图时暂停自动轮播
     */
    public void hideView() {
        if (mCurrentView != null) {
            mCurrentView.setVisibility(View.GONE);
            // 暂停自动轮播
            isAutoSliding.set(false);
        }
    }
    
    /**
     * 读取登录状态
     */
    private boolean readLoginStatus() {
        SharedPreferences sp = mContext.getSharedPreferences("loginInfo", Activity.MODE_PRIVATE);
        return sp.getBoolean("isLogin", false);
    }
    
    /**
     * 打开课程搜索页面
     */
    private void openCourseSearchFragment() {
        try {
            // 使用MainActivity的搜索功能
            if (mContext instanceof com.example.xinqiao.activity.MainActivity) {
                ((com.example.xinqiao.activity.MainActivity) mContext).openCourseSearchFragment();
            }
        } catch (Exception e) {
            android.util.Log.e("CourseView", "打开课程搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 销毁视图，释放资源
     */
    public void onDestroy() {
        // 停止自动轮播
        isAutoSliding.set(false);
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}
