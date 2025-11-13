package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.example.xinqiao.R;
import com.example.xinqiao.util.ImageLoader;


public class AdBannerFragment extends Fragment {
	private String ab;// 广告
	private ImageView iv;// 图片
	public static AdBannerFragment newInstance(Bundle args) {
		AdBannerFragment af = new AdBannerFragment();
		af.setArguments(args);
		return af;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arg = getArguments();
		// 获取广告图片名称
		ab = arg.getString("ad");

	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (ab != null) {
			if ("banner_1".equals(ab)) {
				ImageLoader.loadImage(getContext(), R.drawable.banner_1, iv);
			} else if ("banner_2".equals(ab)) {
				ImageLoader.loadImage(getContext(), R.drawable.banner_2, iv);
			} else if ("banner_3".equals(ab)) {
				ImageLoader.loadImage(getContext(), R.drawable.banner_3, iv);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (iv != null) {
			iv.setImageDrawable(null);
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (iv != null) {
			iv.setImageDrawable(null);
			iv = null;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// 创建广告图片控件
		iv = new ImageView(getActivity());
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT);
		iv.setLayoutParams(lp);// 设置图片宽高参数
		iv.setScaleType(ImageView.ScaleType.FIT_XY);// 把图片塞满整个控件
		
		// 确保每次创建视图时都加载图片
		if (ab != null) {
			if ("banner_1".equals(ab)) {
				ImageLoader.loadImage(getContext(), R.drawable.banner_1, iv);
			} else if ("banner_2".equals(ab)) {
				ImageLoader.loadImage(getContext(), R.drawable.banner_2, iv);
			} else if ("banner_3".equals(ab)) {
				ImageLoader.loadImage(getContext(), R.drawable.banner_3, iv);
			}
		}
		
		return iv;
	}
}