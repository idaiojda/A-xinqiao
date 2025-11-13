-- 更新chat_history表，添加sessionId字段
ALTER TABLE chat_history ADD COLUMN sessionId INT DEFAULT 0;

-- 创建chat_session表
CREATE TABLE IF NOT EXISTS chat_session (
    id INT PRIMARY KEY AUTO_INCREMENT,
    userName VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    createTime BIGINT NOT NULL,
    updateTime BIGINT NOT NULL
);

-- 创建索引
CREATE INDEX idx_chat_history_sessionId ON chat_history(sessionId);
CREATE INDEX idx_chat_session_userName ON chat_session(userName);