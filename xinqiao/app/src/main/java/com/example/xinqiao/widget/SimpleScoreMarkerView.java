package com.example.xinqiao.widget;

import android.content.Context;
import android.widget.TextView;

import com.example.xinqiao.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;

/**
 * 简易分数趋势 MarkerView：显示日期与分数。
 */
public class SimpleScoreMarkerView extends MarkerView {

    private final TextView tvContent;
    private MPPointF mOffset;
    private final List<String> labels; // yyyy-MM-dd

    public SimpleScoreMarkerView(Context context, List<String> labels) {
        super(context, R.layout.marker_score_simple);
        tvContent = findViewById(R.id.tv_marker_content);
        this.labels = labels;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e != null) {
            int idx = Math.round(e.getX());
            String day = (labels != null && idx >= 0 && idx < labels.size()) ? labels.get(idx) : "";
            float score = e.getY();
            tvContent.setText((day != null ? day : "") + "\n分数：" + Math.round(score));
        } else {
            tvContent.setText("--");
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        if (mOffset == null) {
            mOffset = new MPPointF(-(getWidth() / 2f), -getHeight());
        }
        return mOffset;
    }
}

