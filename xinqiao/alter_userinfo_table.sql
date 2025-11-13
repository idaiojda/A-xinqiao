ALTER TABLE userinfo
ADD COLUMN birthday DATE,
ADD COLUMN marital_status VARCHAR(20),
ADD COLUMN occupation VARCHAR(20),
ADD COLUMN introduction TEXT,
ADD COLUMN last_nickname_update DATETIME; 