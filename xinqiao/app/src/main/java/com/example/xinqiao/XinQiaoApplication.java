package com.example.xinqiao;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.mysql.DBUpdater;
import com.example.xinqiao.util.ui.ImageLoader;
import com.example.xinqiao.util.perf.MemoryOptimizer;
import com.example.xinqiao.util.perf.NetworkOptimizer;
import com.example.xinqiao.util.perf.PerformanceMonitor;
import com.example.xinqiao.util.perf.StartupOptimizer;
import com.example.xinqiao.util.perf.ThreadOptimizer;
import com.example.xinqiao.R;

import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import com.example.xinqiao.network.NetworkConfig;
import com.example.xinqiao.community.CommunityRepositoryProvider;
import com.example.xinqiao.community.CommunityServiceFactory;
import com.example.xinqiao.community.RemoteCommunityRepository;
import com.example.xinqiao.community.CommunityApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.xinqiao.community.SettingsRepository;
import com.example.xinqiao.util.AnalysisUtils;

public class XinQiaoApplication extends Application {
    // 日志标签
    private static final String TAG = "XinQiaoApplication";
    private volatile boolean realtimeSubscribedApp = false;

    /**
     * 应用启动时回调，进行全局初始化
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化启动优化器（必须最先初始化）
        initStartupOptimizer();
        try {
            SharedPreferences sp = getSharedPreferences("network_config", MODE_PRIVATE);
            String current = sp.getString("base_url_override", null);
            String desired = (com.example.xinqiao.BuildConfig.BACKEND_URL != null && com.example.xinqiao.BuildConfig.BACKEND_URL.trim().length() > 0)
                    ? com.example.xinqiao.BuildConfig.BACKEND_URL.trim()
                    : "http://10.0.2.2:8081";
            if (current == null || !current.equals(desired)) {
                sp.edit().putString("base_url_override", desired).apply();
                Log.d(TAG, "Network base URL overridden to " + desired);
            }
        } catch (Throwable t) {
            Log.w(TAG, "setup base_url_override failed", t);
        }
        
        configureSSL();      // 配置全局SSL信任管理器
        com.example.xinqiao.community.CommunityLocalCache.INSTANCE.init(this);
        try {
            SettingsRepository.INSTANCE.init(getApplicationContext());
            String userName = AnalysisUtils.readLoginUserName(this);
            SettingsRepository.INSTANCE.get(userName, s -> {
                String lang = (s != null && s.getAppLanguage() != null) ? s.getAppLanguage() : "zh";
                try { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang)); } catch (Throwable ignored) {}
                return kotlin.Unit.INSTANCE;
            });
        } catch (Throwable ignored) {}
        
        // 初始化性能优化相关工具
        initPerformanceMonitor();
        initThreadOptimizer();
        initNetworkOptimizer();
        initMemoryOptimizer();

        initDatabase();

        // 社区模块：接入远端仓库（Retrofit）
        try {
            String baseUrl = NetworkConfig.getBaseUrl(this);
            CommunityApi api = CommunityServiceFactory.INSTANCE.create(baseUrl);
            CommunityRepositoryProvider.INSTANCE.setCurrent(new RemoteCommunityRepository(api));
            Log.d(TAG, "CommunityRepository initialized with remote backend: " + baseUrl);
            try {
                com.example.xinqiao.community.CommunityLocalCache.INSTANCE.purgeSamples();
                Log.d(TAG, "Purged sample posts from local DB");
            } catch (Throwable t2) { Log.w(TAG, "Purge samples failed", t2); }
        } catch (Throwable t) {
            Log.w(TAG, "Init remote CommunityRepository failed, fallback to empty repository", t);
            try {
                CommunityRepositoryProvider.INSTANCE.setCurrent(com.example.xinqiao.community.EmptyCommunityRepository.INSTANCE);
            } catch (Throwable ignored) {}
        }

        try {
            com.example.xinqiao.network.Http.init(this);
            Log.d(TAG, "Http initialized for core network");
        } catch (Throwable t) { Log.w(TAG, "Http init failed", t); }
        
        // 注册应用生命周期回调
        registerLifecycleCallbacks();
        try { com.example.xinqiao.notifications.NotificationUtils.INSTANCE.ensureChannel(this); } catch (Throwable ignored) {}
    }

    private boolean isDebugBuild() {
        try {
            return com.example.xinqiao.BuildConfig.DEBUG;
        } catch (Throwable t) {
            return true; // 安全默认：若读取失败，按调试处理
        }
    }

    private boolean isAndroidEmulator() {
        String fingerprint = Build.FINGERPRINT.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        String device = Build.DEVICE.toLowerCase();
        String product = Build.PRODUCT.toLowerCase();
        String hardware = Build.HARDWARE.toLowerCase();
        return fingerprint.contains("generic") || fingerprint.contains("emulator") ||
                model.contains("emulator") || model.contains("android sdk built for") ||
                (brand.contains("google") && device.contains("generic")) ||
                product.contains("sdk_gphone") || hardware.contains("ranchu") ||
                hardware.contains("goldfish");
    }

    /**
     * 配置全局SSL信任管理器，保证HTTPS请求安全
     */
    private void configureSSL() {
        try {
            // 获取默认的信任管理器工厂
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            // 创建SSLContext并初始化
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            // 设置全局默认的SSLSocketFactory
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            Log.d(TAG, "已成功配置SSL信任管理器");
        } catch (Exception e) {
            Log.e(TAG, "配置SSL时出错", e);
        }
    }

    /**
     * 异步初始化数据库，包括MySQL连接和表结构创建
     */
    private void initDatabase() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    // 初始化MySQLHelper，回调中处理表结构创建
                    MySQLHelper.init(XinQiaoApplication.this, new MySQLHelper.InitCallback() {
                        @Override
                        public void onSuccess() {
                            try {
                                // 初始化成功后创建数据库表
                                // 使用异步方式执行表创建操作，避免阻塞主线程
                                new Thread(() -> {
                                    try {
                                        MySQLHelper.getInstance().createTables();
                                        // 初始化数据库更新工具
                                        new DBUpdater(XinQiaoApplication.this).checkAndUpdateDatabase();
                                        // 主线程提示成功
                                        new Handler(Looper.getMainLooper()).post(() -> Log.d(TAG, "数据库初始化成功"));
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        new Handler(Looper.getMainLooper()).post(() -> Log.e(TAG, "创建数据库表失败: " + e.getMessage()));
                                    }
                                }).start();
                            } catch (IllegalStateException e) {
                                // 如果MySQLHelper尚未初始化完成，等待一段时间后重试
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        MySQLHelper.getInstance().createTables();
                                        // 初始化数据库更新工具
                                        new DBUpdater(XinQiaoApplication.this).checkAndUpdateDatabase();
                                        Log.d(TAG, "数据库初始化成功");
                                    } catch (SQLException | IllegalStateException ex) {
                                        ex.printStackTrace();
                                        Log.e(TAG, "创建数据库表失败: " + ex.getMessage());
                                    }
                                }, 1000);
                            }
                        }
                        @Override
                        public void onError(SQLException e) {
                            e.printStackTrace();
                            new Handler(Looper.getMainLooper()).post(() -> Log.e(TAG, "数据库初始化失败: " + e.getMessage()));
                        }
                    });
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean success) {
                if (!success) {
                    Log.e(TAG, "启动数据库初始化过程失败");
                }
            }
        }.execute();
    }

    /**
     * 初始化图片加载相关配置，优化内存使用
     */
    private void initImageConfigs() {
        // 配置BitmapFactory的默认选项，减少内存占用
        BitmapFactory.Options defaultOptions = new BitmapFactory.Options();
        defaultOptions.inPreferredConfig = Bitmap.Config.RGB_565; // 默认使用RGB_565，降低内存
        defaultOptions.inSampleSize = 1; // 默认采样率，后续可根据图片尺寸调整
        try { defaultOptions.inMutable = true; } catch (Throwable ignored) {}
        // 设置最大Bitmap尺寸，防止Canvas溢出
        int maxBitmapDimension = Math.min(4096, getResources().getDisplayMetrics().widthPixels * 2);
        Log.d(TAG, "Max bitmap dimension set to: " + maxBitmapDimension);
        Log.d(TAG, "Image configurations initialized");
        
        // 在后台线程中预加载常用图片资源，避免阻塞主线程
        ThreadOptimizer.getInstance().executeDiskIO(() -> {
            ImageLoader.preloadImage(XinQiaoApplication.this, R.drawable.loading_placeholder);
            ImageLoader.preloadImage(XinQiaoApplication.this, R.drawable.error_placeholder);
            Log.d(TAG, "图片资源预加载完成");
        });
    }
    
    /**
     * 初始化启动优化器
     */
    private void initStartupOptimizer() {
        StartupOptimizer.getInstance().init(this);
        
        // 添加启动任务
        StartupOptimizer.getInstance()
            // 添加后台任务（非主线程，无延迟）
            .addStartupTask(new StartupOptimizer.BaseStartupTask(false, 0) {
                @Override
                public void execute(android.content.Context context) {
                    Log.d(TAG, "执行后台启动任务");
                    // 非阻塞：图片配置与预加载
                    initImageConfigs();
                }
            })
            // 确保延迟任务在后台线程执行，避免主线程阻塞
            .addStartupTask(new StartupOptimizer.BaseStartupTask(false, 3000) {
                @Override
                public void execute(android.content.Context context) {
                    Log.d(TAG, "执行延迟启动任务");
                    // 非紧急：数据库初始化（MySQL/本地缓存）
                    initDatabase();
                }
            });
            
        Log.d(TAG, "启动优化器初始化完成");
    }
    
    /**
     * 初始化性能监控器
     */
    private void initPerformanceMonitor() {
        PerformanceMonitor.getInstance().init(this);
        
        // 添加性能监听器
        PerformanceMonitor.getInstance().addListener(new PerformanceMonitor.PerformanceListenerAdapter() {
            @Override
            public void onFrameRateWarning(int skippedFrames) {
                Log.w(TAG, "检测到严重掉帧: " + skippedFrames + "帧");
            }
            
            @Override
            public void onANRDetected(long blockTime) {
                Log.e(TAG, "检测到可能的ANR，主线程阻塞时间: " + blockTime + "ms");
            }
        });
        
        Log.d(TAG, "性能监控器初始化完成");
    }
    
    /**
     * 初始化线程优化器
     */
    private void initThreadOptimizer() {
        // 获取单例实例即可完成初始化
        ThreadOptimizer.getInstance();
        Log.d(TAG, "线程优化器初始化完成");
    }
    
    /**
     * 初始化网络优化器
     */
    private void initNetworkOptimizer() {
        // 获取单例实例即可完成初始化
        NetworkOptimizer.getInstance();
        Log.d(TAG, "网络优化器初始化完成");
    }
    
    /**
     * 初始化内存优化器
     */
    private void initMemoryOptimizer() {
        // 注册低内存回调
        this.registerComponentCallbacks(new android.content.ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                    MemoryOptimizer.releaseMemoryOnLowMemory(XinQiaoApplication.this);
                }
            }
            
            @Override
            public void onLowMemory() {
                MemoryOptimizer.releaseMemoryOnLowMemory(XinQiaoApplication.this);
            }
            
            @Override
            public void onConfigurationChanged(android.content.res.Configuration newConfig) {
                // 配置变化时不需要特殊处理
            }
        });
        
        Log.d(TAG, "内存优化器初始化完成");
    }
    
    /**
     * 应用终止时回调，释放资源
     * 注意：此方法在大多数设备上不会被调用，仅用于模拟器和开发环境
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        // 释放性能监控器资源
        PerformanceMonitor.getInstance().release();
        Log.d(TAG, "应用终止，已释放资源");
    }
    
    /**
     * 注册低内存和进程状态监听
     * 在应用进入后台或内存不足时释放资源
     */
    private void registerLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int activityCount = 0;
            
            @Override
            public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {}
            
            @Override
            public void onActivityStarted(android.app.Activity activity) {
                if (activityCount == 0) {
                    // 应用从后台进入前台
                    Log.d(TAG, "应用进入前台");
                    try { com.example.xinqiao.community.GlobalRealtimeReceiver.INSTANCE.onForeground(XinQiaoApplication.this); } catch (Throwable t) { Log.w(TAG, "GlobalRealtimeReceiver onForeground failed", t); }
                }
                activityCount++;
            }
            
            @Override
            public void onActivityResumed(android.app.Activity activity) {}
            
            @Override
            public void onActivityPaused(android.app.Activity activity) {}
            
            @Override
            public void onActivityStopped(android.app.Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    // 应用进入后台，释放非必要资源
                    Log.d(TAG, "应用进入后台，释放资源");
                    PerformanceMonitor.getInstance().stopMonitor();
                    try { com.example.xinqiao.community.GlobalRealtimeReceiver.INSTANCE.onBackground(); realtimeSubscribedApp = false; } catch (Throwable ignored) {}
                }
            }
            
            @Override
            public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {}
            
            @Override
            public void onActivityDestroyed(android.app.Activity activity) {}
        });
    }

    
}
