package com.example.xinqiao.widget;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import com.example.xinqiao.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;
import java.util.Map;

/**
 * 情绪趋势 MarkerView：显示日期、强度等级、记录数与摘要。
 */
public class SimpleEmotionMarkerView extends MarkerView {

    private final TextView tvContent;
    private MPPointF mOffset;

    private final List<String> labels;                  // yyyy-MM-dd
    private final Map<String, Integer> dayCountMap;     // 每日记录数
    private final Map<String, String> dayNoteMap;       // 每日摘要（解密后，可为空）

    public SimpleEmotionMarkerView(Context context, List<String> labels,
                                   Map<String, Integer> dayCountMap,
                                   Map<String, String> dayNoteMap) {
        super(context, R.layout.marker_emotion_simple);
        tvContent = findViewById(R.id.tv_marker_content);
        this.labels = labels;
        this.dayCountMap = dayCountMap;
        this.dayNoteMap = dayNoteMap;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e != null) {
            int bucket = Math.round(e.getY());
            String level;
            if (bucket <= 1) level = "偏低";
            else if (bucket == 2) level = "低";
            else if (bucket == 3) level = "中";
            else level = "高";

            int idx = Math.round(e.getX());
            String day = (labels != null && idx >= 0 && idx < labels.size()) ? labels.get(idx) : "";
            Integer cntObj = (dayCountMap != null && day != null) ? dayCountMap.get(day) : null;
            int count = cntObj != null ? cntObj : 0;

            String note = (dayNoteMap != null && day != null) ? dayNoteMap.get(day) : null;
            if (note != null && note.length() > 36) {
                note = note.substring(0, 36) + "…";
            }

            StringBuilder sb = new StringBuilder();
            if (!TextUtils.isEmpty(day)) {
                sb.append(day).append("  ");
            }
            sb.append("强度 ").append(bucket).append("（").append(level).append("）");
            if (count > 1) {
                sb.append(" | ").append(count).append("条均值");
            }
            if (!TextUtils.isEmpty(note)) {
                sb.append("\n摘要：").append(note);
            }
            tvContent.setText(sb.toString());
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
