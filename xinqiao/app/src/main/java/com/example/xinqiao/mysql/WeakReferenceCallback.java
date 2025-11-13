package com.example.xinqiao.mysql;

/**
 * 弱引用回调接口，用于检查回调对象是否仍然有效
 * 实现此接口的回调可以在Activity销毁后避免执行UI操作
 */
public interface WeakReferenceCallback {
    /**
     * 检查回调对象是否仍然有效
     * @return 如果回调对象仍然有效，返回true；否则返回false
     */
    boolean isAlive();
}