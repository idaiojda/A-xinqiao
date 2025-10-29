package com.example.xinqiao.mysql;

import android.content.Context;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class MySQLHelper {
    private Context context;
    private static MySQLHelper instance;
    private BlockingQueue<Connection> connectionPool;
    private String url;
    private String username;
    private String password;
    private String dbName;
    private static final int CONNECTION_TIMEOUT_SECONDS = 15; // 缩短连接超时时间
    private static final int MAX_RETRY_COUNT = 3; // 减少重试次数，避免过长等待
    private static final int INITIAL_RETRY_DELAY = 1000; // 初始重试延迟（毫秒）
    private static final int MAX_RETRY_DELAY = 5000; // 减少最大重试延迟（毫秒）
    private int poolSize; // 从配置文件读取连接池大小

    private MySQLHelper(Context context) throws SQLException {
        this.context = context.getApplicationContext();
        try {
            DatabaseConfig.init(this.context);
        } catch (IOException e) {
            throw new SQLException("加载数据库配置文件失败", e);
        }
        
        // 记录数据库配置信息
        String host = DatabaseConfig.getHost();
        String port = DatabaseConfig.getPort();
        this.dbName = DatabaseConfig.getDatabaseName();
        android.util.Log.d("MySQLHelper", String.format("尝试连接数据库: %s:%s/%s", host, port, this.dbName));
        
        // 验证数据库配置
        if (host == null || host.isEmpty() || port == null || port.isEmpty() || dbName == null || dbName.isEmpty()) {
            throw new SQLException("数据库配置无效: 主机、端口或数据库名称不能为空");
        }
        
        // 检查网络连接
        if (!isNetworkConnected()) {
            throw new SQLException("网络连接不可用，请检查网络设置");
        }
        
        // 修改连接URL参数，增加更多调试选项和优化连接参数
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + this.dbName + 
            "?useSSL=false" + 
            "&allowPublicKeyRetrieval=true" + 
            "&characterEncoding=utf8" + 
            "&connectTimeout=30000" +  // 减少连接超时时间
            "&socketTimeout=60000" +   // 减少套接字超时时间
            "&autoReconnect=true" + 
            "&failOverReadOnly=false" + 
            "&maxReconnects=5" +       // 减少重连次数
            "&serverTimezone=GMT%2B8" + 
            "&tcpKeepAlive=true" + 
            "&useCompression=true" +    // 启用压缩
            "&useUnicode=true" +       // 确保使用Unicode
            "&zeroDateTimeBehavior=convertToNull" + // 处理零日期
            "&useLocalSessionState=true" + // 优化性能
            "&cacheServerConfiguration=true" + // 缓存服务器配置
            "&cachePrepStmts=true" +   // 缓存预处理语句
            "&prepStmtCacheSize=250" + // 预处理语句缓存大小
            "&prepStmtCacheSqlLimit=2048" + // SQL限制
            "&rewriteBatchedStatements=true" +
            "&connectionCollation=utf8mb4_0900_ai_ci"; // 添加这一行
        
        // 记录连接URL
        android.util.Log.d("MySQLHelper", "数据库连接URL: " + this.url);
        this.username = DatabaseConfig.getUsername();
        this.password = DatabaseConfig.getPassword();
        
        this.poolSize = DatabaseConfig.getInitialPoolSize();
        this.connectionPool = new ArrayBlockingQueue<>(this.poolSize);
        initializePool();
        android.util.Log.i("MySQLHelper", String.format("数据库连接池初始化成功，连接池大小: %d", this.poolSize));
    }

    public static void init(Context context, InitCallback callback) {
        new Thread(() -> {
            try {
                if (instance == null) {
                    synchronized (MySQLHelper.class) {
                        if (instance == null) {
                            instance = new MySQLHelper(context);
                        }
                    }
                }
                if (callback != null) {
                    // 确保在主线程回调
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess());
                }
            } catch (SQLException e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError(e));
                }
            }
        }).start();
    }

    public interface InitCallback {
        void onSuccess();
        void onError(SQLException e);
    }

    public static MySQLHelper getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MySQLHelper not initialized, please call init method first");
        }
        return instance;
    }
    
    public static void getInstance(Context context, InitCallback callback) {
        if (instance == null) {
            init(context, callback);
        } else {
            // 确保在主线程回调
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess());
        }
    }

    private void initializePool() throws SQLException {
        try {
            // 加载数据库驱动
            android.util.Log.d("MySQLHelper", "正在加载MySQL驱动...");
            Class.forName("com.mysql.jdbc.Driver");
            android.util.Log.i("MySQLHelper", "MySQL驱动加载成功");
            
            // 测试数据库服务器是否可达
            testDatabaseServerReachable();
            
            // 预热连接池，提前建立最小数量的连接
            int minConnections = DatabaseConfig.getMinIdleConnections();
            android.util.Log.d("MySQLHelper", String.format("开始预热连接池，最小连接数: %d", minConnections));
            
            int successfulConnections = 0;
            SQLException lastException = null;
            
            for (int attempt = 1; attempt <= MAX_RETRY_COUNT && successfulConnections < minConnections; attempt++) {
                android.util.Log.d("MySQLHelper", String.format("尝试建立数据库连接 (第%d次尝试)...", attempt));
                
                try {
                    // 尝试建立连接直到达到最小连接数
                    while (successfulConnections < minConnections) {
                        Connection conn = createNewConnection();
                        if (conn != null && conn.isValid(3)) {
                            connectionPool.offer(conn);
                            successfulConnections++;
                            android.util.Log.d("MySQLHelper", String.format("成功创建连接 %d/%d", successfulConnections, minConnections));
                        }
                    }
                    
                    // 如果达到最小连接数，初始化成功
                    if (successfulConnections >= minConnections) {
                        android.util.Log.i("MySQLHelper", String.format("连接池预热成功，已创建 %d 个连接", successfulConnections));
                        return;
                    }
                } catch (SQLException e) {
                    lastException = e;
                    android.util.Log.e("MySQLHelper", String.format("第%d次尝试创建连接失败: %s", attempt, e.getMessage()));
                    
                    if (attempt < MAX_RETRY_COUNT) {
                        try {
                            int retryDelay = Math.min(INITIAL_RETRY_DELAY * (int)Math.pow(2, attempt - 1), MAX_RETRY_DELAY);
                            android.util.Log.i("MySQLHelper", String.format("等待 %d 毫秒后重试...", retryDelay));
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new SQLException("连接重试过程被中断", ie);
                        }
                    }
                }
            }
            
            // 如果所有尝试都失败
            String errorMsg = lastException != null ? 
                "数据库连接失败: " + lastException.getMessage() : 
                "无法创建数据库连接池，请检查数据库配置和网络连接";
            android.util.Log.e("MySQLHelper", "所有连接尝试均失败: " + errorMsg);
            throw new SQLException(errorMsg);
            
        } catch (ClassNotFoundException e) {
            android.util.Log.e("MySQLHelper", "MySQL驱动加载失败: " + e.getMessage());
            throw new SQLException("MySQL驱动加载失败: " + e.getMessage());
        }
    }

    public interface ConnectionCallback {
        void onSuccess(Connection connection);
        void onError(SQLException e);
    }

    public void getConnection(ConnectionCallback callback) {
        new Thread(() -> {
            try {
                android.util.Log.d("MySQLHelper", "尝试从连接池获取连接...");
                Connection connection = connectionPool.poll(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                if (connection == null) {
                    android.util.Log.d("MySQLHelper", "连接池中无可用连接，创建新连接");
                    connection = createNewConnection();
                } else {
                    // 验证连接是否有效
                    try {
                        if (!connection.isValid(3)) {
                            android.util.Log.d("MySQLHelper", "从连接池获取的连接无效，关闭并创建新连接");
                            try { connection.close(); } catch (SQLException ex) { /* 忽略关闭错误 */ }
                            connection = createNewConnection();
                        } else {
                            android.util.Log.d("MySQLHelper", "从连接池获取的连接有效");
                        }
                    } catch (SQLException e) {
                        android.util.Log.w("MySQLHelper", "验证连接有效性时出错: " + e.getMessage());
                        try { connection.close(); } catch (SQLException ex) { /* 忽略关闭错误 */ }
                        connection = createNewConnection();
                    }
                }
                
                final Connection finalConnection = connection;
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(finalConnection));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onError(new SQLException("等待数据库连接时被中断", e)));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void getConnection(final ConnectionResultCallback callback) {
        new Thread(() -> {
            try {
                // 尝试从连接池获取连接
                android.util.Log.d("MySQLHelper", "尝试从连接池获取连接...");
                Connection conn = null;
                synchronized (connectionPool) {
                    conn = connectionPool.poll();
                    if (conn != null && isConnectionValid(conn)) {
                        android.util.Log.d("MySQLHelper", "从连接池获取的连接有效");
                        callback.onSuccess(conn);
                        return;
                    } else if (conn != null) {
                        android.util.Log.d("MySQLHelper", "从连接池获取的连接无效，将创建新连接");
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // 如果连接池为空或连接无效，创建新连接
                android.util.Log.d("MySQLHelper", "创建新数据库连接...");
                conn = createNewConnection();
                if (conn != null) {
                    android.util.Log.d("MySQLHelper", "新连接创建成功");
                    callback.onSuccess(conn);
                } else {
                    android.util.Log.e("MySQLHelper", "创建新连接失败");
                    callback.onError(new SQLException("无法创建数据库连接"));
                }
            } catch (Exception e) {
                android.util.Log.e("MySQLHelper", "获取连接时发生异常: " + e.getMessage());
                callback.onError(new SQLException("获取连接失败: " + e.getMessage()));
            }
        }).start();
    }

    public interface ConnectionResultCallback {
        void onSuccess(Connection connection);
        void onError(SQLException e);
    }

    public Connection getConnection() throws SQLException {
        Connection connection = null;
        int attempts = 0;
        int maxAttempts = 3;
        
        while (attempts < maxAttempts) {
            try {
                android.util.Log.d("MySQLHelper", String.format("尝试获取连接 (第%d次)...", attempts + 1));
                connection = connectionPool.poll(5, TimeUnit.SECONDS);
                
                if (connection == null) {
                    android.util.Log.d("MySQLHelper", "连接池为空，创建新连接");
                    connection = createNewConnection();
                }
                
                if (connection != null) {
                    if (connection.isValid(2)) {
                        android.util.Log.d("MySQLHelper", "获取到有效连接");
                        return connection;
                    } else {
                        android.util.Log.d("MySQLHelper", "连接无效，关闭并重试");
                        try { connection.close(); } catch (SQLException e) { /* ignore */ }
                        connection = null;
                    }
                }
            } catch (SQLException e) {
                android.util.Log.e("MySQLHelper", String.format("获取连接失败 (尝试%d): %s", attempts + 1, e.getMessage()));
                if (connection != null) {
                    try { connection.close(); } catch (SQLException ce) { /* ignore */ }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("等待连接时被中断", e);
            }
            
            attempts++;
            if (attempts < maxAttempts) {
                try {
                    Thread.sleep(1000 * attempts); // 递增重试延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("重试等待时被中断", e);
                }
                android.util.Log.d("MySQLHelper", String.format("准备第%d次重试...", attempts + 1));
            }
        }
        
        throw new SQLException(String.format("无法获取有效连接，已重试%d次", maxAttempts));
    }

    private Connection createNewConnection() throws SQLException {
        for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
            try {
                if (!isNetworkConnected()) {
                    android.util.Log.e("MySQLHelper", "网络未连接，无法创建数据库连接");
                    throw new SQLException("网络未连接");
                }

                Class.forName("com.mysql.jdbc.Driver");
                android.util.Log.d("MySQLHelper", "尝试连接数据库: " + this.url);
                
                // 设置连接属性
                java.util.Properties props = new java.util.Properties();
                props.setProperty("user", DatabaseConfig.getUsername());
                props.setProperty("password", DatabaseConfig.getPassword());
                props.setProperty("connectTimeout", "30000");
                props.setProperty("socketTimeout", "60000");
                
                Connection conn = DriverManager.getConnection(this.url, props);
                if (conn != null) {
                    android.util.Log.d("MySQLHelper", "数据库连接成功");
                    return conn;
                }
            } catch (Exception e) {
                android.util.Log.e("MySQLHelper", "创建连接失败 (尝试 " + (retryCount + 1) + "/" + MAX_RETRY_COUNT + "): " + e.getMessage());
                if (retryCount < MAX_RETRY_COUNT - 1) {
                    try {
                        Thread.sleep(INITIAL_RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new SQLException("无法创建数据库连接，已重试 " + MAX_RETRY_COUNT + " 次");
    }

    public void releaseConnection(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            // 快速检查连接是否已关闭
            if (connection.isClosed()) {
                android.util.Log.d("MySQLHelper", "连接已关闭，不再归还到连接池");
                return;
            }

            // 验证连接是否有效，使用较短的超时时间
            if (connection.isValid(1)) {
                // 检查连接池是否已满
                if (connectionPool.size() < poolSize) {
                    if (connectionPool.offer(connection, 2, TimeUnit.SECONDS)) {
                        android.util.Log.d("MySQLHelper", "连接已成功归还到连接池");
                        return;
                    } else {
                        android.util.Log.d("MySQLHelper", "归还连接超时，将关闭连接");
                    }
                } else {
                    android.util.Log.d("MySQLHelper", "连接池已满，将关闭多余连接");
                }
            } else {
                android.util.Log.d("MySQLHelper", "连接无效，将关闭");
            }

            // 如果连接无效、连接池已满或归还超时，则关闭连接
            connection.close();
        } catch (SQLException e) {
            android.util.Log.e("MySQLHelper", "处理连接释放时发生错误: " + e.getMessage());
            try {
                connection.close();
            } catch (SQLException ex) {
                android.util.Log.e("MySQLHelper", "关闭连接失败: " + ex.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            android.util.Log.e("MySQLHelper", "归还连接时被中断");
            try {
                connection.close();
            } catch (SQLException ex) {
                android.util.Log.e("MySQLHelper", "关闭连接失败: " + ex.getMessage());
            }
        }
    }

    public void closeConnection() {
        android.util.Log.i("MySQLHelper", "开始关闭所有数据库连接...");
        List<Connection> connectionsToClose = new ArrayList<>();
        
        // 从连接池中取出所有连接
        connectionPool.drainTo(connectionsToClose);
        int totalConnections = connectionsToClose.size();
        AtomicInteger closedConnections = new AtomicInteger(0);
        
        // 并行关闭所有连接
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(totalConnections, 5));
        List<Future<?>> futures = new ArrayList<>();
        
        for (Connection conn : connectionsToClose) {
            futures.add(executor.submit(() -> {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                        int current = closedConnections.incrementAndGet();
                        android.util.Log.d("MySQLHelper", String.format("成功关闭连接 (%d/%d)", current, totalConnections));
                    }
                } catch (SQLException e) {
                    android.util.Log.e("MySQLHelper", "关闭连接失败: " + e.getMessage());
                }
            }));
        }
        
        // 等待所有连接关闭完成
        for (Future<?> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                android.util.Log.e("MySQLHelper", "等待连接关闭时发生错误: " + e.getMessage());
            }
        }
        
        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        
        android.util.Log.i("MySQLHelper", String.format("数据库连接池已关闭，共关闭 %d 个连接", closedConnections.get()));
    }

    private boolean isNetworkConnected() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private boolean testDatabaseServerReachable() {
        try {
            java.net.Socket socket = new java.net.Socket();
            String host = DatabaseConfig.getHost();
            int port = Integer.parseInt(DatabaseConfig.getPort());
            socket.connect(new java.net.InetSocketAddress(host, port), 5000);
            socket.close();
            android.util.Log.i("MySQLHelper", "数据库服务器可达");
            return true;
        } catch (Exception e) {
            android.util.Log.e("MySQLHelper", "无法连接到数据库服务器: " + e.getMessage());
            return false;
        }
    }

    private boolean isConnectionValid(Connection connection) {
        try {
            return connection != null && connection.isValid(3);
        } catch (SQLException e) {
            android.util.Log.e("MySQLHelper", "验证连接有效性时出错: " + e.getMessage());
            return false;
        }
    }

    // 创建数据库表
    public void createTables() throws SQLException {
        android.util.Log.d("MySQLHelper", "开始创建数据库表...");
        long startTime = System.currentTimeMillis();
        
        Connection conn = null;
        try {
            conn = getConnection();
            
            // 首先创建用户信息表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS user_info (" +
                "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(50) NOT NULL, " +
                "nickname VARCHAR(50), " +
                "gender VARCHAR(10), " +
                "birthday DATE, " +
                "marital_status VARCHAR(20), " +
                "occupation VARCHAR(50), " +
                "introduction TEXT, " +
                "avatar LONGBLOB, " +
                "balance DECIMAL(10,2) DEFAULT 0.00, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            
            // 确保balance列存在
            try {
                android.util.Log.d("MySQLHelper", "开始检查balance列是否存在...");
                // 检查balance列是否存在
                String checkBalanceColumn = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = '" + dbName + "' AND table_name = 'user_info' AND column_name = 'balance'";
                android.util.Log.d("MySQLHelper", "执行SQL: " + checkBalanceColumn);
                ResultSet rs = conn.createStatement().executeQuery(checkBalanceColumn);
                rs.next();
                boolean hasBalanceColumn = rs.getInt(1) > 0;
                android.util.Log.d("MySQLHelper", "balance列存在: " + hasBalanceColumn);
                
                if (!hasBalanceColumn) {
                    // 如果balance列不存在，添加该列
                    String addBalanceColumn = "ALTER TABLE user_info ADD COLUMN balance DECIMAL(10,2) DEFAULT 0.00";
                    android.util.Log.d("MySQLHelper", "执行SQL: " + addBalanceColumn);
                    conn.createStatement().executeUpdate(addBalanceColumn);
                    android.util.Log.i("MySQLHelper", "已添加balance列到user_info表");
                } else {
                    android.util.Log.i("MySQLHelper", "balance列已存在，无需添加");
                }
                
                // 检查是否有balance为null的用户并初始化
                String checkNullBalance = "SELECT COUNT(*) FROM user_info WHERE balance IS NULL";
                android.util.Log.d("MySQLHelper", "执行SQL: " + checkNullBalance);
                rs = conn.createStatement().executeQuery(checkNullBalance);
                rs.next();
                int nullBalanceCount = rs.getInt(1);
                android.util.Log.d("MySQLHelper", "balance为null的用户数: " + nullBalanceCount);
                
                if (nullBalanceCount > 0) {
                    // 为balance为null的用户设置默认余额0.00
                    String updateBalance = "UPDATE user_info SET balance = 0.00 WHERE balance IS NULL";
                    android.util.Log.d("MySQLHelper", "执行SQL: " + updateBalance);
                    conn.createStatement().executeUpdate(updateBalance);
                    android.util.Log.i("MySQLHelper", "已为" + nullBalanceCount + "个用户初始化余额");
                } else {
                    android.util.Log.i("MySQLHelper", "没有balance为null的用户，无需初始化");
                }
                
                // 验证balance列是否可以正常查询
                try {
                    String testQuery = "SELECT username, balance FROM user_info LIMIT 1";
                    android.util.Log.d("MySQLHelper", "测试balance列查询: " + testQuery);
                    rs = conn.createStatement().executeQuery(testQuery);
                    if (rs.next()) {
                        String username = rs.getString("username");
                        double balance = rs.getDouble("balance");
                        android.util.Log.i("MySQLHelper", "测试查询成功: 用户 " + username + " 余额为 " + balance);
                    } else {
                        android.util.Log.i("MySQLHelper", "测试查询成功，但没有用户数据");
                    }
                } catch (SQLException e) {
                    android.util.Log.e("MySQLHelper", "测试balance列查询失败: " + e.getMessage());
                }
            } catch (SQLException e) {
                android.util.Log.e("MySQLHelper", "检查或添加balance列时出错: " + e.getMessage());
                e.printStackTrace();
            }
            // 其他表的创建

            // 创建视频播放记录表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS videoplaylist (" +
                "_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "userName VARCHAR(50), " +
                "chapterId INT, " +
                "videoId INT, " +
                "videoPath VARCHAR(200), " +
                "title VARCHAR(100), " +
                "secondTitle VARCHAR(100)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            // 创建AI聊天历史记录表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS chat_history (" +
                "_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "userName VARCHAR(50), " +
                "content TEXT, " +
                "type INT, " +
                "timestamp BIGINT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            // 创建文章阅读历史记录表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS article_history (" +
                "_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "userName VARCHAR(50), " +
                "articleId INT, " +
                "title VARCHAR(200), " +
                "content TEXT, " +
                "category VARCHAR(50), " +
                "readTimestamp BIGINT, " +
                "readProgress INT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            // 创建课程购买记录表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS course_purchase (" +
                "purchase_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "course_id INT NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "purchase_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES user_info(user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // 创建课程价格表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS course_price (" +
                "course_id INT PRIMARY KEY, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "title VARCHAR(100) NOT NULL, " +
                "description TEXT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
                
            // 创建习题购买记录表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS exercise_purchase (" +
                "purchase_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "exercise_id INT NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "purchase_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES user_info(user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // 初始化课程价格数据
            String checkSql = "SELECT COUNT(*) as count FROM course_price";
            ResultSet coursePriceRs = conn.createStatement().executeQuery(checkSql);
            coursePriceRs.next();
            if (coursePriceRs.getInt("count") == 0) {
                // 插入默认课程价格
                String insertSql = "INSERT INTO course_price (course_id, price, title) VALUES (?, 99.00, ?)";
                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                
                // 从assets/chaptertitle.xml读取的课程ID为1-12
                for (int i = 1; i <= 12; i++) {
                    pstmt.setInt(1, i);
                    pstmt.setString(2, "心理课程 " + i);
                    pstmt.executeUpdate();
                }
            }

            // 创建搜索历史表（如不存在）
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS search_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "userName VARCHAR(50) NOT NULL, " +
                "keyword VARCHAR(100) NOT NULL, " +
                "createTime BIGINT NOT NULL, " +
                "INDEX idx_search_history_userName (userName), " +
                "INDEX idx_search_history_createTime (createTime)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
            
            long endTime = System.currentTimeMillis();
            android.util.Log.i("MySQLHelper", "数据库表创建完成，耗时: " + (endTime - startTime) + "ms");
        } catch (SQLException e) {
            android.util.Log.e("MySQLHelper", "创建数据库表失败: " + e.getMessage());
            throw e;
        } finally {
            if (conn != null) {
                releaseConnection(conn);
            }
        }
    }
}
