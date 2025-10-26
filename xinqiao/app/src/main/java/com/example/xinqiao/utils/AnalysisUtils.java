package com.example.xinqiao.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import com.example.xinqiao.bean.CourseBean;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AnalysisUtils {
    private static final String PREF_NAME = "loginInfo";
    private static final String KEY_LOGIN_USERNAME = "loginUserName";
    private static final String KEY_LOGIN_USER_ID = "loginUserId";

    /**
     * 解析每章的课程视频信息
     */
    public static List<List<CourseBean>> getCourseInfos(InputStream is) throws Exception {
        XmlPullParser parser= Xml.newPullParser();
        parser.setInput(is, "utf-8");
        List<List<CourseBean>> courseInfos=null;
        List<CourseBean> courseList=null;
        CourseBean courseInfo=null;
        int count=0;
        int type=parser.getEventType();
        while (type!=XmlPullParser.END_DOCUMENT) {
            switch (type) {
                case XmlPullParser.START_TAG:
                    if("infos".equals(parser.getName())){
                        courseInfos=new ArrayList<List<CourseBean>>();
                        courseList=new ArrayList<CourseBean>();
                    }else if("course".equals(parser.getName())){
                        courseInfo=new CourseBean();
                        String ids=parser.getAttributeValue(0);
                        courseInfo.id=Integer.parseInt(ids);
                    }else if("imgtitle".equals(parser.getName())){
                        String imgtitle=parser.nextText();
                        courseInfo.imgTitle=imgtitle;
                    }else if("title".equals(parser.getName())){
                        String title=parser.nextText();
                        courseInfo.title=title;
                    }else if("intro".equals(parser.getName())){
                        String intro=parser.nextText();
                        courseInfo.intro=intro;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if("course".equals(parser.getName())){
                        count++;
                        courseList.add(courseInfo);
                        if(count%2==0){// 课程界面每两个数据是一组放在List集合中
                            courseInfos.add(courseList);
                            courseList=null;
                            courseList=new ArrayList<CourseBean>();
                        }
                        courseInfo=null;
                    }
                    break;
            }
            type=parser.next();
        }
        return courseInfos;
    }
    /**
     * 从SharedPreferences中读取登录用户名
     * @param context 上下文
     * @return 登录用户名，如果未登录或用户名无效则返回空字符串
     */
    public static String readLoginUserName(Context context) {
        if (context == null) {
            android.util.Log.e("AnalysisUtils", "readLoginUserName: context为空");
            return "";
        }
        
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userName = sp.getString(KEY_LOGIN_USERNAME, "");
        
        // 记录读取到的用户名
        if (userName == null || userName.trim().isEmpty()) {
            android.util.Log.w("AnalysisUtils", "readLoginUserName: 未找到登录用户名或用户名为空");
            return "";
        } else {
            android.util.Log.d("AnalysisUtils", "readLoginUserName: 读取到登录用户名=" + userName);
            return userName.trim(); // 确保返回的用户名没有前后空格
        }
    }

    /**
     * 从SharedPreferences中读取登录用户ID
     * @param context 上下文
     * @return 登录用户ID，如果未登录或ID无效则返回-1
     */
    public static int readUserId(Context context) {
        if (context == null) {
            android.util.Log.e("AnalysisUtils", "readUserId: context为空");
            return -1;
        }

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int userId = sp.getInt(KEY_LOGIN_USER_ID, -1);

        if (userId == -1) {
            android.util.Log.w("AnalysisUtils", "readUserId: 未找到登录用户ID或ID无效");
        } else {
            android.util.Log.d("AnalysisUtils", "readUserId: 读取到登录用户ID=" + userId);
        }
        return userId;
    }

    /**
     * 保存登录信息（用户名和用户ID）到SharedPreferences
     * @param context 上下文
     * @param userName 用户名
     * @param userId 用户ID
     */
    public static void saveLoginInfo(Context context, String userName, int userId) {
        if (context == null) {
            android.util.Log.e("AnalysisUtils", "saveLoginInfo: context为空");
            return;
        }
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_LOGIN_USERNAME, userName);
        editor.putInt(KEY_LOGIN_USER_ID, userId);
        editor.putBoolean("isLogin", true);
        editor.apply();
        android.util.Log.d("AnalysisUtils", "saveLoginInfo: 已保存用户名=" + userName + ", 用户ID=" + userId);
    }

    /**
     * 清除登录信息
     */
    public static void clearLoginInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
        android.util.Log.d("AnalysisUtils", "clearLoginInfo: 已清除登录信息");
    }
}
