package com.example.xinqiao.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网络优化工具类
 * 提供一系列方法来优化网络请求
 */
public class NetworkOptimizer {
    private static final int DEFAULT_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int DEFAULT_CONNECT_TIMEOUT = 15; // 15秒
    private static final int DEFAULT_READ_TIMEOUT = 20; // 20秒
    private static final int DEFAULT_WRITE_TIMEOUT = 20; // 20秒
    
    private static NetworkOptimizer instance;
    private final Map<String, List<NetworkRequest>> batchRequests = new HashMap<>();
    
    private NetworkOptimizer() {}
    
    public static synchronized NetworkOptimizer getInstance() {
        if (instance == null) {
            instance = new NetworkOptimizer();
        }
        return instance;
    }
    
    /**
     * 检查网络连接状态
     * @param context 上下文
     * @return 如果网络连接可用返回true，否则返回false
     */
    public boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * 创建网络缓存目录
     * @param context 上下文
     * @return 缓存目录
     */
    public File createCacheDir(Context context) {
        File cacheDir = new File(context.getCacheDir(), "network_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }
    
    /**
     * 添加批量请求
     * @param batchKey 批量请求的键
     * @param request 网络请求
     */
    public void addBatchRequest(String batchKey, NetworkRequest request) {
        if (batchKey == null || request == null) {
            return;
        }
        
        List<NetworkRequest> requests = batchRequests.get(batchKey);
        if (requests == null) {
            requests = new ArrayList<>();
            batchRequests.put(batchKey, requests);
        }
        requests.add(request);
    }
    
    /**
     * 执行批量请求
     * @param batchKey 批量请求的键
     * @param callback 批量请求回调
     */
    public void executeBatchRequests(String batchKey, BatchRequestCallback callback) {
        if (batchKey == null || callback == null) {
            return;
        }
        
        List<NetworkRequest> requests = batchRequests.get(batchKey);
        if (requests == null || requests.isEmpty()) {
            callback.onBatchComplete(new ArrayList<>());
            return;
        }
        
        // 在后台线程执行批量请求
        ThreadOptimizer.getInstance().executeNetworkIO(() -> {
            List<NetworkResponse> responses = new ArrayList<>();
            
            for (NetworkRequest request : requests) {
                try {
                    // 模拟网络请求
                    Thread.sleep(100);
                    NetworkResponse response = new NetworkResponse(true, "Success", null);
                    responses.add(response);
                } catch (Exception e) {
                    NetworkResponse response = new NetworkResponse(false, null, e.getMessage());
                    responses.add(response);
                }
            }
            
            // 在主线程返回结果
            ThreadOptimizer.getInstance().executeMainThread(() -> {
                callback.onBatchComplete(responses);
                // 清除已处理的请求
                batchRequests.remove(batchKey);
            });
        });
    }
    
    /**
     * 网络请求类
     */
    public static class NetworkRequest {
        private final String url;
        private final String method;
        private final Map<String, String> headers;
        private final String body;
        
        public NetworkRequest(String url, String method, Map<String, String> headers, String body) {
            this.url = url;
            this.method = method;
            this.headers = headers;
            this.body = body;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getMethod() {
            return method;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public String getBody() {
            return body;
        }
    }
    
    /**
     * 网络响应类
     */
    public static class NetworkResponse {
        private final boolean success;
        private final String data;
        private final String error;
        
        public NetworkResponse(boolean success, String data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getData() {
            return data;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * 批量请求回调接口
     */
    public interface BatchRequestCallback {
        void onBatchComplete(List<NetworkResponse> responses);
    }
}