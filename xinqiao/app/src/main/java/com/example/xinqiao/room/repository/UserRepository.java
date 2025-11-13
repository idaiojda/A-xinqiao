package com.example.xinqiao.room.repository;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.xinqiao.room.AppDatabase;
import com.example.xinqiao.room.dao.UserInfoDao;
import com.example.xinqiao.room.entity.UserInfo;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用户数据仓库类，封装对用户数据的访问操作
 */
public class UserRepository {
    
    private final UserInfoDao userInfoDao;
    private final ExecutorService executorService;
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        userInfoDao = db.userInfoDao();
        executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 插入用户信息
     * @param userInfo 用户信息实体
     * @param callback 回调接口
     */
    public void insert(UserInfo userInfo, final OperationCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                // 设置创建时间和更新时间
                Date now = new Date();
                userInfo.setCreatedAt(now);
                userInfo.setUpdatedAt(now);
                
                long id = userInfoDao.insert(userInfo);
                if (callback != null) {
                    callback.onSuccess(id);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新用户信息
     * @param userInfo 用户信息实体
     * @param callback 回调接口
     */
    public void update(UserInfo userInfo, final OperationCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                // 设置更新时间
                userInfo.setUpdatedAt(new Date());
                
                int rows = userInfoDao.update(userInfo);
                if (callback != null) {
                    callback.onSuccess(rows);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 删除用户信息
     * @param userInfo 用户信息实体
     * @param callback 回调接口
     */
    public void delete(UserInfo userInfo, final OperationCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = userInfoDao.delete(userInfo);
                if (callback != null) {
                    callback.onSuccess(rows);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @param callback 回调接口
     */
    public void getUserById(int userId, final OperationCallback<UserInfo> callback) {
        executorService.execute(() -> {
            try {
                UserInfo userInfo = userInfoDao.getUserById(userId);
                if (callback != null) {
                    callback.onSuccess(userInfo);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @param callback 回调接口
     */
    public void getUserByUsername(String username, final OperationCallback<UserInfo> callback) {
        executorService.execute(() -> {
            try {
                UserInfo userInfo = userInfoDao.getUserByUsername(username);
                if (callback != null) {
                    callback.onSuccess(userInfo);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取所有用户信息
     * @param callback 回调接口
     */
    public void getAllUsers(final OperationCallback<List<UserInfo>> callback) {
        executorService.execute(() -> {
            try {
                List<UserInfo> userInfoList = userInfoDao.getAllUsers();
                if (callback != null) {
                    callback.onSuccess(userInfoList);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取所有用户信息（LiveData版本，支持数据变化监听）
     * @return 包含用户信息实体列表的LiveData
     */
    public LiveData<List<UserInfo>> getAllUsersLive() {
        return userInfoDao.getAllUsersLive();
    }
    
    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码
     * @param callback 回调接口
     */
    public void login(String username, String password, final OperationCallback<UserInfo> callback) {
        executorService.execute(() -> {
            try {
                UserInfo userInfo = userInfoDao.login(username, password);
                if (callback != null) {
                    callback.onSuccess(userInfo);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新用户余额
     * @param userId 用户ID
     * @param balance 新余额
     * @param callback 回调接口
     */
    public void updateBalance(int userId, double balance, final OperationCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = userInfoDao.updateBalance(userId, balance);
                if (callback != null) {
                    callback.onSuccess(rows);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取用户余额
     * @param userId 用户ID
     * @param callback 回调接口
     */
    public void getBalance(int userId, final OperationCallback<Double> callback) {
        executorService.execute(() -> {
            try {
                double balance = userInfoDao.getBalance(userId);
                if (callback != null) {
                    callback.onSuccess(balance);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatar 头像数据
     * @param callback 回调接口
     */
    public void updateAvatar(int userId, byte[] avatar, final OperationCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = userInfoDao.updateAvatar(userId, avatar);
                if (callback != null) {
                    callback.onSuccess(rows);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取用户头像
     * @param userId 用户ID
     * @param callback 回调接口
     */
    public void getAvatar(int userId, final OperationCallback<byte[]> callback) {
        executorService.execute(() -> {
            try {
                byte[] avatar = userInfoDao.getAvatar(userId);
                if (callback != null) {
                    callback.onSuccess(avatar);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 更新用户基本信息
     * @param userId 用户ID
     * @param nickname 昵称
     * @param gender 性别
     * @param introduction 简介
     * @param callback 回调接口
     */
    public void updateBasicInfo(int userId, String nickname, String gender, String introduction, final OperationCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = userInfoDao.updateBasicInfo(userId, nickname, gender, introduction);
                if (callback != null) {
                    callback.onSuccess(rows);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 清空用户表
     * @param callback 回调接口
     */
    public void deleteAllUsers(final OperationCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = userInfoDao.deleteAllUsers();
                if (callback != null) {
                    callback.onSuccess(rows);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 操作回调接口
     * @param <T> 返回数据类型
     */
    public interface OperationCallback<T> {
        /**
         * 操作成功回调
         * @param result 操作结果
         */
        void onSuccess(T result);
        
        /**
         * 操作失败回调
         * @param e 异常信息
         */
        void onError(Exception e);
    }
}