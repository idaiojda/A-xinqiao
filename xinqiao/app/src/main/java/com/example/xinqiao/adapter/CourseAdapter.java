package com.example.xinqiao.adapter;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.VideoListActivity;
import com.example.xinqiao.bean.CourseBean;

import java.util.List;

public class CourseAdapter extends BaseAdapter {
	// 上下文对象
	private Context mContext;
	// 课程数据列表，外层List为行，内层List为每行的课程（一般2个）
	private List<List<CourseBean>> cbl;

	/**
	 * 构造方法，初始化上下文
	 */
	public CourseAdapter(Context context) {
		this.mContext = context;
	}
	/**
	 * 设置数据并刷新界面
	 */
	public void setData(List<List<CourseBean>> cbl) {
		this.cbl = cbl;
		notifyDataSetChanged();
	}
	/**
	 * 获取Item的总数（即行数）
	 */
	@Override
	public int getCount() {
		return cbl == null ? 0 : cbl.size();
	}
	/**
	 * 根据position得到对应Item的对象（每行的课程列表）
	 */
	@Override
	public List<CourseBean> getItem(int position) {
		return cbl == null ? null : cbl.get(position);
	}
	/**
	 * 根据position得到对应Item的id
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}
	/**
	 * 得到相应position对应的Item视图，
	 * position是当前Item的位置，
	 * convertView参数就是滚出屏幕的Item的View
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder vh;
		if (convertView == null) {
			vh = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.course_list_item, null);
			vh.iv_left_img = (ImageView) convertView
					.findViewById(R.id.iv_left_img);
			vh.iv_right_img = (ImageView) convertView
					.findViewById(R.id.iv_right_img);
			vh.tv_left_img_title = (TextView) convertView
					.findViewById(R.id.tv_left_img_title);
			vh.tv_left_title = (TextView) convertView
					.findViewById(R.id.tv_left_title);
			vh.tv_right_img_title = (TextView) convertView
					.findViewById(R.id.tv_right_img_title);
			vh.tv_right_title = (TextView) convertView
					.findViewById(R.id.tv_right_title);
			convertView.setTag(vh);
		} else {
			// 复用convertView，提升性能
			vh = (ViewHolder) convertView.getTag();
		}
		final List<CourseBean> list = getItem(position);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				final CourseBean bean = list.get(i);
				switch (i) {
					case 0:// 设置左边图片与标题信息
						vh.tv_left_img_title.setText(bean.imgTitle);
						vh.tv_left_title.setText(bean.title);
						setLeftImg(bean.id, vh.iv_left_img);
						vh.iv_left_img.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								// 跳转到课程详情界面
								Intent intent = new Intent(mContext,
										VideoListActivity.class);
								intent.putExtra("id", bean.id);
								intent.putExtra("intro", bean.intro);
								mContext.startActivity(intent);
							}
						});
						break;
					case 1:// 设置右边图片与标题信息
						vh.tv_right_img_title.setText(bean.imgTitle);
						vh.tv_right_title.setText(bean.title);
						setRightImg(bean.id, vh.iv_right_img);
						vh.iv_right_img.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								// 跳转到课程详情界面
								Intent intent = new Intent(mContext,
										VideoListActivity.class);
								intent.putExtra("id", bean.id);
								intent.putExtra("intro", bean.intro);
								mContext.startActivity(intent);
							}
						});
						break;
					default:
						break;
				}
			}
		}
		return convertView;
	}
	/**
	 * 设置左边图片，根据课程id选择不同图片
	 */
	private void setLeftImg(int id, ImageView iv_left_img) {
		switch (id) {
			case 1:
				iv_left_img.setImageResource(R.mipmap.chapter_1_icon);
				break;
			case 3:
				iv_left_img.setImageResource(R.mipmap.chapter_3_icon);
				break;
			case 5:
				iv_left_img.setImageResource(R.mipmap.chapter_5_icon);
				break;
			case 7:
				iv_left_img.setImageResource(R.mipmap.chapter_7_icon);
				break;
			case 9:
				iv_left_img.setImageResource(R.mipmap.chapter_9_icon);
				break;
		}
	}
	/**
	 * 设置右边图片，根据课程id选择不同图片
	 */
	private void setRightImg(int id, ImageView iv_right_img) {
		switch (id) {
			case 2:
				iv_right_img.setImageResource(R.mipmap.chapter_2_icon);
				break;
			case 4:
				iv_right_img.setImageResource(R.mipmap.chapter_4_icon);
				break;
			case 6:
				iv_right_img.setImageResource(R.mipmap.chapter_6_icon);
				break;
			case 8:
				iv_right_img.setImageResource(R.mipmap.chapter_8_icon);
				break;
			case 10:
				iv_right_img.setImageResource(R.mipmap.chapter_10_icon);
				break;
		}
	}
	/**
	 * ViewHolder内部类，缓存item视图，提升性能
	 */
	class ViewHolder {
		public TextView tv_left_img_title, tv_left_title, tv_right_img_title,
				tv_right_title;
		public ImageView iv_left_img, iv_right_img;
	}
}
