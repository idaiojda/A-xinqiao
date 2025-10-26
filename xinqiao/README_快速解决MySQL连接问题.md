# MySQL连接问题快速解决方案

## 问题概述

应用程序日志显示：
- 数据库服务器可达（Socket连接成功）
- 但无法创建数据库连接（`Could not create connection to database server. Attempted reconnect 10 times. Giving up.`）

## 快速检查清单

### 1. 确认MySQL服务器状态

- [ ] MySQL服务是否正在运行？
- [ ] 使用以下命令检查：`sc query mysql`

### 2. 检查用户权限

- [ ] 确认root用户是否有远程连接权限
- [ ] 在MySQL中执行：
  ```sql
  CREATE USER 'root'@'%' IDENTIFIED BY '123456';
  GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
  FLUSH PRIVILEGES;
  ```

### 3. 确认数据库存在

- [ ] 确认`xinqiao`数据库是否已创建
- [ ] 在MySQL中执行：
  ```sql
  CREATE DATABASE IF NOT EXISTS xinqiao CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### 4. 检查网络设置

- [ ] 如果使用模拟器：确认10.0.2.2可以访问主机MySQL
- [ ] 如果使用真机：修改`database.properties`中的主机地址为实际IP
- [ ] 检查防火墙是否允许3306端口通信

## 应用程序改进

我们已经对应用程序进行了以下改进：

1. 优化了数据库连接参数
2. 添加了更详细的连接验证步骤
3. 增加了更多调试日志
4. 改进了连接属性设置方式

## 测试连接

可以使用以下命令测试从命令行连接MySQL：

```
mysql -h 127.0.0.1 -u root -p123456 -D xinqiao
```

如果命令行可以连接但应用程序不能，请检查应用程序日志中的详细错误信息，特别是新增的用户凭据验证和数据库存在性验证结果。