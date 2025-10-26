package com.example.xinqiao.utils;

import android.text.TextUtils;
import java.util.regex.Pattern;

/**
 * 手机号验证工具类
 */
public class PhoneUtils {
    
    // 中国手机号正则表达式：1[3-9]xxxxxxxxx
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * 验证手机号格式是否正确
     * @param phone 手机号
     * @return true表示格式正确，false表示格式错误
     */
    public static boolean isValidPhoneNumber(String phone) {
        return !TextUtils.isEmpty(phone) && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * 验证手机号是否为空
     * @param phone 手机号
     * @return true表示为空，false表示不为空
     */
    public static boolean isEmpty(String phone) {
        return TextUtils.isEmpty(phone);
    }
    
    /**
     * 格式化手机号显示（隐藏中间4位）
     * @param phone 手机号
     * @return 格式化后的手机号，如：138****8888
     */
    public static String formatPhoneForDisplay(String phone) {
        if (!isValidPhoneNumber(phone)) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 获取手机号运营商信息
     * @param phone 手机号
     * @return 运营商名称
     */
    public static String getCarrier(String phone) {
        if (!isValidPhoneNumber(phone)) {
            return "未知";
        }
        
        String prefix = phone.substring(0, 3);
        
        // 中国移动
        if (prefix.equals("134") || prefix.equals("135") || prefix.equals("136") || 
            prefix.equals("137") || prefix.equals("138") || prefix.equals("139") ||
            prefix.equals("147") || prefix.equals("150") || prefix.equals("151") ||
            prefix.equals("152") || prefix.equals("157") || prefix.equals("158") ||
            prefix.equals("159") || prefix.equals("178") || prefix.equals("182") ||
            prefix.equals("183") || prefix.equals("184") || prefix.equals("187") ||
            prefix.equals("188") || prefix.equals("198")) {
            return "中国移动";
        }
        
        // 中国联通
        if (prefix.equals("130") || prefix.equals("131") || prefix.equals("132") ||
            prefix.equals("145") || prefix.equals("155") || prefix.equals("156") ||
            prefix.equals("166") || prefix.equals("175") || prefix.equals("176") ||
            prefix.equals("185") || prefix.equals("186")) {
            return "中国联通";
        }
        
        // 中国电信
        if (prefix.equals("133") || prefix.equals("149") || prefix.equals("153") ||
            prefix.equals("173") || prefix.equals("177") || prefix.equals("180") ||
            prefix.equals("181") || prefix.equals("189") || prefix.equals("199")) {
            return "中国电信";
        }
        
        // 虚拟运营商
        if (prefix.equals("170") || prefix.equals("171")) {
            return "虚拟运营商";
        }
        
        return "未知";
    }
} 