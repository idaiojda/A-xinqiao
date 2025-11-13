package com.example.xinqiao.widget;

import android.content.Context;
import android.widget.TextView;

import com.example.xinqiao.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;
import java.util.Map;

/**
 * 富信息情绪趋势 MarkerView：显示日期、强度、记录数与摘要。
 */
public class RichEmotionMarkerView extends MarkerView {

    private final TextView tvContent;
    private MPPointF mOffset;
    private final List<String> labels;
    private final Map<String, Integer> dayCountMap;
    private final Map<String, String> dayNoteMap;

    public RichEmotionMarkerView(Context context,
                                 List<String> labels,
                                 Map<String, Integer> dayCountMap,
                                 Map<String, String> dayNoteMap) {
        super(context, R.layout.marker_emotion_simple);
        this.tvContent = findViewById(R.id.tv_marker_content);
        this.labels = labels;
        this.dayCountMap = dayCountMap;
        this.dayNoteMap = dayNoteMap;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e != null) {
            int idx = Math.round(e.getX());
            String dayKey = null;
            String dateLabel = "";
            if (labels != null && idx >= 0 && idx < labels.size()) {
                dayKey = labels.get(idx);
                if (dayKey != null && dayKey.length() >= 10) {
                    dateLabel = dayKey.substring(5);
                } else if (dayKey != null) {
                    dateLabel = dayKey;
                }
            }

            int bucket = Math.round(e.getY());
            String level;
            if (bucket <= 1) level = "偏低";
            else if (bucket == 2) level = "低";
            else if (bucket == 3) level = "中";
            else level = "高";

            int count = 0;
            if (dayKey != null && dayCountMap != null) {
                Integer c = dayCountMap.get(dayKey);
                if (c != null) count = c;
            }

            String note = null;
            if (dayKey != null && dayNoteMap != null) {
                note = dayNoteMap.get(dayKey);
            }
            String noteLine = "";
            if (note != null && !note.isEmpty()) {
                String t = note.length() > 40 ? (note.substring(0, 40) + "…") : note;
                noteLine = "\n" + t;
            }

            String content = (dateLabel.isEmpty() ? "" : (dateLabel + " | "))
                    + "强度 " + bucket + "（" + level + "）"
                    + " · 记录 " + count
                    + noteLine;
            tvContent.setText(content);
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

