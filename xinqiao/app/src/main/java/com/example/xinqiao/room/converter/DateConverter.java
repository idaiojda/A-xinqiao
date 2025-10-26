package com.example.xinqiao.room.converter;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Date类型转换器，用于Room数据库中Date类型与Long类型的相互转换
 */
public class DateConverter {
    
    /**
     * 将Date类型转换为Long类型（时间戳）
     * @param date Date对象
     * @return 时间戳（毫秒）
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    /**
     * 将Long类型（时间戳）转换为Date类型
     * @param timestamp 时间戳（毫秒）
     * @return Date对象
     */
    @TypeConverter
    public static Date timestampToDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }
}