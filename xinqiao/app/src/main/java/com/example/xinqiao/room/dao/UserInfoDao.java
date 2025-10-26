package com.example.xinqiao.room.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.xinqiao.room.entity.UserInfo;

import java.util.List;

/**
 * UserInfoDao接口，定义对UserInfo实体的数据库操作
 */
@Dao
public interface UserInfoDao {
    
    /**
     * 插入用户信息
     * @param userInfo 用户信息实体
     * @return 插入的行ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserInfo userInfo);
    
    /**
     * 更新用户信息
     * @param userInfo 用户信息实体
     * @return 更新的行数
     */
    @Update
    int update(UserInfo userInfo);
    
    /**
     * 删除用户信息
     * @param userInfo 用户信息实体
     * @return 删除的行数
     */
    @Delete
    int delete(UserInfo userInfo);
    
    /**
     * 根据用户ID查询用户信息
     * @param userId 用户ID
     * @return 用户信息实体
     */
    @Query("SELECT * FROM user_info WHERE user_id = :userId")
    UserInfo getUserById(int userId);
    
    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户信息实体
     */
    @Query("SELECT * FROM user_info WHERE username = :username")
    UserInfo getUserByUsername(String username);
    
    /**
     * 获取所有用户信息
     * @return 用户信息实体列表
     */
    @Query("SELECT * FROM user_info")
    List<UserInfo> getAllUsers();
    
    /**
     * 获取所有用户信息（LiveData版本，支持数据变化监听）
     * @return 包含用户信息实体列表的LiveData
     */
    @Query("SELECT * FROM user_info")
    LiveData<List<UserInfo>> getAllUsersLive();
    
    /**
     * 根据用户名和密码查询用户信息（用于登录验证）
     * @param username 用户名
     * @param password 密码
     * @return 用户信息实体
     */
    @Query("SELECT * FROM user_info WHERE username = :username AND password = :password")
    UserInfo login(String username, String password);
    
    /**
     * 更新用户余额
     * @param userId 用户ID
     * @param balance 新余额
     * @return 更新的行数
     */
    @Query("UPDATE user_info SET balance = :balance WHERE user_id = :userId")
    int updateBalance(int userId, double balance);
    
    /**
     * 获取用户余额
     * @param userId 用户ID
     * @return 用户余额
     */
    @Query("SELECT COALESCE(balance, 0.00) FROM user_info WHERE user_id = :userId")
    double getBalance(int userId);
    
    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatar 头像数据
     * @return 更新的行数
     */
    @Query("UPDATE user_info SET avatar = :avatar WHERE user_id = :userId")
    int updateAvatar(int userId, byte[] avatar);
    
    /**
     * 获取用户头像
     * @param userId 用户ID
     * @return 头像数据
     */
    @Query("SELECT avatar FROM user_info WHERE user_id = :userId")
    byte[] getAvatar(int userId);
    
    /**
     * 更新用户基本信息
     * @param userId 用户ID
     * @param nickname 昵称
     * @param gender 性别
     * @param introduction 简介
     * @return 更新的行数
     */
    @Query("UPDATE user_info SET nickname = :nickname, gender = :gender, introduction = :introduction WHERE user_id = :userId")
    int updateBasicInfo(int userId, String nickname, String gender, String introduction);
    
    /**
     * 清空用户表
     * @return 删除的行数
     */
    @Query("DELETE FROM user_info")
    int deleteAllUsers();
}