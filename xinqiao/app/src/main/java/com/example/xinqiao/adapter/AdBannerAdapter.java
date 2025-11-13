package com.example.xinqiao.adapter;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.xinqiao.bean.CourseBean;
import com.example.xinqiao.fragment.AdBannerFragment;
import com.example.xinqiao.view.CourseView;

import java.util.ArrayList;
import java.util.List;

public class AdBannerAdapter extends FragmentStatePagerAdapter implements
		OnTouchListener {
	// 轮播图自动滑动的Handler
	private Handler mHandler;
	// 广告Banner数据列表
	private List<CourseBean> cadl;
	// 当前选中位置
	private int currentPosition = 0;

	/**
	 * 构造方法，初始化FragmentManager
	 */
	public AdBannerAdapter(FragmentManager fm) {
		super(fm);
		cadl = new ArrayList<CourseBean>();
	}
	/**
	 * 构造方法，支持传入Handler用于自动滑动控制
	 */
	public AdBannerAdapter(FragmentManager fm, Handler handler) {
		super(fm);
		mHandler = handler;
		cadl = new ArrayList<CourseBean>();
	}
	/**
	 * 设置数据并刷新界面
	 */
	public void setDatas(List<CourseBean> cadl) {
		this.cadl = cadl;
		notifyDataSetChanged();
	}
	/**
	 * 获取指定位置的Fragment，支持无限循环（通过取模）
	 */
	@Override
	public Fragment getItem(int index) {
		Bundle args = new Bundle();
		if (cadl.size() > 0)
			args.putString("ad", cadl.get(index % cadl.size()).icon);
		return AdBannerFragment.newInstance(args);
	}
	/**
	 * 返回极大值，实现无限轮播效果
	 */
	@Override
	public int getCount() {
		// 限制最大数量以减少内存消耗
		return cadl == null || cadl.size() == 0 ? 0 : Integer.MAX_VALUE / 2;
	}
	/**
	 * 返回数据集的真实容量大小
	 */
	public int getSize() {
		return cadl == null ? 0 : cadl.size();
	}
	/**
	 * 防止刷新时出现缓存数据，强制每次都重新加载
	 */
	@Override
	public int getItemPosition(Object object) {
		// 优化缓存策略，仅当数据发生变化时才重新加载
		return POSITION_NONE;
	}
	/**
	 * 触摸轮播图时，移除自动滑动消息，暂停自动轮播
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (mHandler != null) {
			mHandler.removeMessages(CourseView.MSG_AD_SLID);
		}
		return false;
	}
}