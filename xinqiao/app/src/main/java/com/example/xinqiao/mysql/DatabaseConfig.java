package com.example.xinqiao.mysql;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static Properties properties;
    
    public static void init(Context context) throws IOException {
        if (properties != null) {
            android.util.Log.d("DatabaseConfig", "数据库配置已加载，跳过初始化");
            return;
        }
        
        properties = new Properties();
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("database.properties");
            properties.load(inputStream);
            
            // 验证必要的配置项
            String[] requiredProps = {"db.host", "db.port", "db.name", "db.user", "db.password"};
            for (String prop : requiredProps) {
                if (!properties.containsKey(prop)) {
                    throw new IOException("缺少必要的配置项: " + prop);
                }
            }
            
            android.util.Log.i("DatabaseConfig", "成功加载数据库配置文件");
            android.util.Log.d("DatabaseConfig", String.format("数据库配置: 主机=%s, 端口=%s, 数据库名=%s",
                getHost(), getPort(), getDatabaseName()));
            inputStream.close();
        } catch (IOException e) {
            properties = null;
            android.util.Log.e("DatabaseConfig", "加载数据库配置文件失败: " + e.getMessage());
            throw e;
        }
    }
    
    public static String getHost() {
        return properties.getProperty("db.host", "localhost");
    }
    
    public static String getPort() {
        return properties.getProperty("db.port", "3306");
    }
    
    public static String getDatabaseName() {
        return properties.getProperty("db.name", "xinqiao");
    }
    
    public static String getUsername() {
        return properties.getProperty("db.user", "root");
    }
    
    public static String getPassword() {
        return properties.getProperty("db.password", "123456");
    }
    
    public static int getInitialPoolSize() {
        return Integer.parseInt(properties.getProperty("db.pool.initialSize", "5"));
    }
    
    public static int getMaxPoolSize() {
        return Integer.parseInt(properties.getProperty("db.pool.maxActive", "10"));
    }
    
    public static int getMaxIdleConnections() {
        return Integer.parseInt(properties.getProperty("db.pool.maxIdle", "5"));
    }
    
    public static int getMinIdleConnections() {
        return Integer.parseInt(properties.getProperty("db.pool.minIdle", "2"));
    }
    
    public static int getMaxWaitMillis() {
        return Integer.parseInt(properties.getProperty("db.pool.maxWait", "60000"));
    }
}