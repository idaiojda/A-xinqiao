# 找回密码功能说明

## 功能概述

本项目已实现完整的手机号找回密码功能，用户可以通过手机号验证码的方式重置密码。

## 功能特点

### 1. 安全性
- **手机号格式验证**：使用正则表达式验证手机号格式（1[3-9]xxxxxxxxx）
- **密码强度检查**：要求密码包含大小写字母和数字，长度至少8位
- **验证码机制**：6位数字验证码，60秒倒计时
- **数据库连接检查**：确保数据库连接可用

### 2. 用户体验
- **直观的界面设计**：现代化的Material Design风格
- **实时反馈**：输入验证、错误提示、成功提示
- **倒计时功能**：防止频繁发送验证码
- **返回按钮**：方便用户返回登录页面

### 3. 技术实现
- **异步数据库操作**：避免阻塞主线程
- **字符串资源化**：支持多语言
- **错误处理**：完善的异常处理机制

## 使用流程

1. **进入找回密码页面**
   - 在登录页面点击"忘记密码？"链接
   - 或直接启动 `FindPasswordActivity`

2. **输入手机号**
   - 输入注册时使用的手机号
   - 系统会验证手机号格式

3. **获取验证码**
   - 点击"获取验证码"按钮
   - 系统生成6位随机验证码
   - 按钮进入60秒倒计时状态

4. **输入验证码和新密码**
   - 输入收到的验证码
   - 输入新密码（需符合强度要求）
   - 点击"确认"按钮

5. **完成密码重置**
   - 系统验证信息并更新数据库
   - 显示成功提示并返回登录页面

## 文件结构

```
app/src/main/
├── java/com/example/xinqiao/activity/
│   └── FindPasswordActivity.java          # 找回密码活动
├── res/layout/
│   └── activity_find_password.xml        # 找回密码页面布局
├── res/values/
│   └── strings.xml                       # 字符串资源
└── AndroidManifest.xml                   # 活动注册
```

## 数据库操作

### 表结构
```sql
-- 用户信息表
CREATE TABLE user_info (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,  -- 手机号作为用户名
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    gender VARCHAR(10),
    introduction TEXT,
    avatar LONGBLOB,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 密码重置方法
```java
// DBUtils.java 中的方法
public boolean updateUserPasswordByPhone(String phone, String newPassword)
```

## 配置要求

### 1. 数据库配置
确保 `app/src/main/assets/database.properties` 文件配置正确：
```properties
db.host=10.0.2.2
db.port=3306
db.name=xinqiao
db.user=root
db.password=123456
```

### 2. 网络权限
已在 `AndroidManifest.xml` 中配置：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 测试建议

### 1. 功能测试
- 测试无效手机号格式
- 测试未注册的手机号
- 测试错误的验证码
- 测试弱密码
- 测试正常流程

### 2. 边界测试
- 网络断开情况
- 数据库连接失败
- 快速点击按钮
- 验证码过期

### 3. 性能测试
- 大量并发请求
- 数据库连接池压力测试

## 扩展建议

### 1. 短信服务集成
当前验证码直接显示在Toast中，实际生产环境应集成短信服务：
```java
// 示例：集成阿里云短信服务
public void sendSMS(String phone, String code) {
    // 调用短信API发送验证码
}
```

### 2. 验证码存储优化
建议将验证码存储在服务器端，而不是客户端：
```java
// 服务器端验证码存储
public boolean verifyCode(String phone, String code) {
    // 从服务器验证验证码
}
```

### 3. 密码加密
建议对密码进行加密存储：
```java
// 使用BCrypt加密
public String encryptPassword(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
}
```

## 注意事项

1. **安全性**：当前实现仅用于演示，生产环境需要加强安全措施
2. **验证码**：实际使用中应通过短信服务发送验证码
3. **密码加密**：建议使用BCrypt等加密算法
4. **错误处理**：可以根据需要添加更详细的错误日志
5. **国际化**：已支持字符串资源化，便于多语言支持

## 联系方式

如有问题或建议，请联系开发团队。 