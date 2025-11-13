package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.content.Intent;
import androidx.viewpager2.widget.ViewPager2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.xinqiao.R;
import com.example.xinqiao.activity.ExercisesSearchActivity;
import com.example.xinqiao.activity.MainActivity;
import com.example.xinqiao.dao.ChatHistoryDao;
import com.example.xinqiao.dao.ChatSessionDao;
import com.example.xinqiao.dao.TestRecordDao;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.bean.TestRecord;
import com.example.xinqiao.repository.MedicalRecordRepository;
import com.example.xinqiao.room.entities.EmotionDiaryEntity;
import com.example.xinqiao.activity.AuthorizationActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.example.xinqiao.widget.SimpleEmotionMarkerView;
import android.graphics.Color;
import com.example.xinqiao.util.CryptoUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MedicalRecordOverviewFragment extends Fragment {

    private TextView tvFinishedCount;
    private TextView tvPendingCount;
    private TextView tvUnfinishedCount;
    private TextView tvChatSessionCount;
    private TextView tvChatMessageCount;
    private LinearLayout hotListContainer;
    private TestRecordDao testRecordDao;
    private ChatSessionDao chatSessionDao;
    private ChatHistoryDao chatHistoryDao;
    private String userName;
    private MedicalRecordRepository repo;
    private LineChart emotionChart;
    private TextView tvEmotionTrendTitle;
    private int currentRangeDays = 30;
    private List<EmotionDiaryEntity> diariesCache;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medical_overview, container, false);
        initViews(view);
        initDataAndStats();
        initEmotionRangeUI(view);
        initEmotionTrend(view);
        initActions(view);
        return view;
    }

    private void initViews(View view) {
        tvFinishedCount = view.findViewById(R.id.tv_finished_count);
        tvPendingCount = view.findViewById(R.id.tv_pending_count);
        tvUnfinishedCount = view.findViewById(R.id.tv_unfinished_count);
        tvChatSessionCount = view.findViewById(R.id.tv_chat_session_count);
        tvChatMessageCount = view.findViewById(R.id.tv_chat_message_count);
        hotListContainer = view.findViewById(R.id.hot_list_container);
        emotionChart = view.findViewById(R.id.line_emotion_trend);
        tvEmotionTrendTitle = view.findViewById(R.id.tv_emotion_trend_title);
    }

    private void initDataAndStats() {
        if (getContext() == null) return;
        userName = AnalysisUtils.readLoginUserName(requireContext());
        testRecordDao = new TestRecordDao(requireContext());
        chatSessionDao = new ChatSessionDao(requireContext());
        chatHistoryDao = new ChatHistoryDao(requireContext());
        repo = new MedicalRecordRepository(requireContext());

        // 测评统计：已完成、待支付、未完成
        if (userName == null || userName.isEmpty()) {
            setTextSafely(tvFinishedCount, "0");
            setTextSafely(tvPendingCount, "0");
            setTextSafely(tvUnfinishedCount, "0");
            setTextSafely(tvChatSessionCount, "0");
            setTextSafely(tvChatMessageCount, "0");
        } else {
            testRecordDao.getTestRecordsByStatusAsync(userName, 1, new TestRecordDao.TestRecordCallback() {
                @Override
                public void onSuccess(List<TestRecord> records) {
                    setTextSafely(tvFinishedCount, String.valueOf(records != null ? records.size() : 0));
                }
                @Override
                public void onError(Exception e) {
                    setTextSafely(tvFinishedCount, "0");
                }
            });

            testRecordDao.getTestRecordsByStatusAsync(userName, 2, new TestRecordDao.TestRecordCallback() {
                @Override
                public void onSuccess(List<TestRecord> records) {
                    setTextSafely(tvPendingCount, String.valueOf(records != null ? records.size() : 0));
                }
                @Override
                public void onError(Exception e) {
                    setTextSafely(tvPendingCount, "0");
                }
            });

            testRecordDao.getTestRecordsByStatusAsync(userName, 0, new TestRecordDao.TestRecordCallback() {
                @Override
                public void onSuccess(List<TestRecord> records) {
                    setTextSafely(tvUnfinishedCount, String.valueOf(records != null ? records.size() : 0));
                }
                @Override
                public void onError(Exception e) {
                    setTextSafely(tvUnfinishedCount, "0");
                }
            });

            // 会话与消息统计
            chatSessionDao.getChatSessionsAsync(userName, sessions -> setTextSafely(tvChatSessionCount, String.valueOf(sessions != null ? sessions.size() : 0)));
            chatHistoryDao.getChatHistoryAsync(userName, history -> setTextSafely(tvChatMessageCount, String.valueOf(history != null ? history.size() : 0)));
        }

        // 热门测评 TOP3
        testRecordDao.getGlobalHotRankAsync(3, new TestRecordDao.TitleCountCallback() {
            @Override
            public void onSuccess(List<TestRecordDao.TitleCount> list) {
                if (hotListContainer == null) return;
                hotListContainer.removeAllViews();
                if (list == null || list.isEmpty()) {
                    addHotItemView("暂无数据", 0);
                    return;
                }
                for (TestRecordDao.TitleCount item : list) {
                    addHotItemView(item.title, item.count);
                }
            }
            @Override
            public void onError(Exception e) {
                if (hotListContainer != null) {
                    hotListContainer.removeAllViews();
                    addHotItemView("加载失败", 0);
                }
            }
        });
    }

    private void initEmotionTrend(View view) {
        LineChart chart = view.findViewById(R.id.line_emotion_trend);
        if (chart == null) return;
        if (userName == null) userName = AnalysisUtils.readLoginUserName(requireContext());

        repo.getEmotionDiariesAsync(userName != null ? userName : "", false, new MedicalRecordRepository.EmotionDiariesCallback() {
            @Override
            public void onSuccess(List<EmotionDiaryEntity> diaries) {
                requireActivity().runOnUiThread(() -> {
                    diariesCache = diaries;
                    renderEmotionTrend(chart, diariesCache, currentRangeDays);
                });
            }

            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> chart.setNoDataText("暂无近30天情绪数据"));
            }
        });
    }

    private void renderEmotionTrend(LineChart chart, List<EmotionDiaryEntity> diaries, int days) {
        if (chart == null) return;
        if (tvEmotionTrendTitle != null) {
            tvEmotionTrendTitle.setText("近" + days + "天情绪趋势");
        }

        // 1) 按日聚合为平均值（解决同一天多条记录的计算偏差）
        Map<String, Float> daySum = new HashMap<>();
        Map<String, Integer> dayCount = new HashMap<>();
        Map<String, String> dayNote = new HashMap<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (diaries != null) {
            for (EmotionDiaryEntity e : diaries) {
                String day = e.date; // yyyy-MM-dd
                int bucket = moodValue(e.mood);
                Float s = daySum.get(day);
                Integer c = dayCount.get(day);
                daySum.put(day, (s == null ? 0f : s) + bucket);
                dayCount.put(day, (c == null ? 0 : c) + 1);
                // 每日摘要：取当天第一条记录的解密内容
                if (!dayNote.containsKey(day)) {
                    String note = CryptoUtil.decrypt(e.noteEncrypted);
                    if (note != null && !note.isEmpty()) {
                        dayNote.put(day, note);
                    }
                }
            }
        }

        // 2) 生成近N天的序列，并进行缺失补全（前向/后向），设置基线
        long now = System.currentTimeMillis();
        List<String> labels = new ArrayList<>();
        float[] raw = new float[days];      // 有记录天的原始均值
        boolean[] hasRaw = new boolean[days];
        for (int i = days - 1; i >= 0; i--) {
            long ts = now - i * 24L * 60L * 60L * 1000L;
            String day = fmt.format(new Date(ts));
            labels.add(day);
            Float s = daySum.get(day);
            Integer c = dayCount.get(day);
            int x = days - 1 - i;
            if (s != null && c != null && c > 0) {
                raw[x] = s / c;  // 当天的平均桶值
                hasRaw[x] = true;
            }
        }

        // 若所有天都缺失，直接提示无数据
        boolean anyRaw = false;
        for (boolean b : hasRaw) { if (b) { anyRaw = true; break; } }
        if (!anyRaw) {
            chart.setNoDataText("暂无近" + days + "天情绪数据");
            chart.invalidate();
            return;
        }

        // 不再补全缺失日期，避免造成“捏造数据”的误解
        // 仅对存在真实记录的日期生成点位，并基于有效点位计算均线

        // 3) 动态移动平均平滑（7天→3日，30天→5日）
        int win = (days == 7) ? 3 : 5;
        List<Entry> entries = new ArrayList<>();
        List<Entry> maEntries = new ArrayList<>();
        for (int x = 0; x < days; x++) {
            if (!hasRaw[x]) continue; // 跳过无数据的日期
            entries.add(new Entry(x, raw[x]));
            int start = Math.max(0, x - win + 1);
            float sum = 0f;
            int count = 0;
            for (int k = start; k <= x; k++) {
                if (hasRaw[k]) { sum += raw[k]; count++; }
            }
            float ma = count > 0 ? (sum / count) : raw[x];
            maEntries.add(new Entry(x, ma));
        }

        LineDataSet dataSet = new LineDataSet(entries, "当天情绪强度");
        dataSet.setColor(0xFF30B4FF);
        dataSet.setCircleColor(0xFF30B4FF);
        dataSet.setLineWidth(2.2f);
        dataSet.setCircleRadius(3.5f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(0x6630B4FF);
        dataSet.setFillAlpha(100);
        // 若仅有一个数据点，关闭填充并改为直线，避免出现“整块填充”错觉
        if (entries.size() < 2) {
            dataSet.setDrawFilled(false);
            dataSet.setMode(LineDataSet.Mode.LINEAR);
        }
        dataSet.setHighlightEnabled(true);
        String maLabel = win + "日均线";
        LineDataSet maSet = new LineDataSet(maEntries, maLabel);
        maSet.setColor(0xFFFF5722); // 更醒目的深橙色
        maSet.setLineWidth(3f);     // 加粗线宽以提升可见性
        maSet.setDrawCircles(false);
        maSet.setMode(LineDataSet.Mode.LINEAR);
        maSet.enableDashedLine(12f, 6f, 0f); // 更明显的虚线节奏
        maSet.setDrawValues(false);

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(1f);
        left.setAxisMaximum(4f);
        left.setDrawGridLines(true);
        left.setLabelCount(4, true);
        left.setTextColor(Color.parseColor("#6B7280"));
        left.setAxisLineColor(Color.parseColor("#E5E7EB"));
        left.setGridColor(Color.parseColor("#E5E7EB"));
        left.setGridLineWidth(0.7f);
        left.enableGridDashedLine(8f, 4f, 0f);
        LimitLine ll2 = new LimitLine(2f, "偏低");
        ll2.setLineWidth(1f);
        ll2.enableDashedLine(10f, 10f, 0f);
        ll2.setTextSize(10f);
        LimitLine ll3 = new LimitLine(3f, "适中");
        ll3.setLineWidth(1f);
        ll3.enableDashedLine(10f, 10f, 0f);
        ll3.setTextSize(10f);
        left.removeAllLimitLines();
        left.addLimitLine(ll2);
        left.addLimitLine(ll3);
        left.setDrawLimitLinesBehindData(true);
        chart.getAxisRight().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(days - 1);
        // 动态刻度步长与刻度数量：仅保留7/30天
        int step = (days == 7) ? 1 : 5;
        int ticks = Math.max(2, (days + step - 1) / step);
        xAxis.setGranularity(step);
        xAxis.setLabelCount(ticks, true);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setTextColor(Color.parseColor("#6B7280"));
        xAxis.setAxisLineColor(Color.parseColor("#E5E7EB"));
        // 显示为 MM-dd，避免年份噪音；按索引返回对应日期
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int idx = Math.round(value);
                if (idx >= 0 && idx < labels.size()) {
                    String d = labels.get(idx);
                    return d.length() >= 10 ? d.substring(5) : d;
                }
                return "";
            }
        });

        chart.getDescription().setEnabled(false);
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        chart.setBackgroundColor(Color.parseColor("#FAFAFA"));
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setExtraOffsets(8f, 8f, 8f, 8f);

        LineData lineData = new LineData();
        lineData.addDataSet(dataSet);
        lineData.addDataSet(maSet);
        chart.setData(lineData);
        chart.setHighlightPerTapEnabled(true);
        chart.setMarker(new SimpleEmotionMarkerView(requireContext(), labels, dayCount, dayNote));
        chart.animateX(600);
        chart.invalidate();
    }

    private void initEmotionRangeUI(View view) {
        RadioGroup rg = view.findViewById(R.id.rg_emotion_range);
        if (rg == null) return;
        RadioButton rb7 = view.findViewById(R.id.rb_range_7);
        RadioButton rb30 = view.findViewById(R.id.rb_range_30);
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            int days = 30;
            if (checkedId == R.id.rb_range_7) days = 7;
            else if (checkedId == R.id.rb_range_30) days = 30;
            currentRangeDays = days;
            // 更新标题文案：近N天情绪趋势
            if (tvEmotionTrendTitle != null) {
                setTextSafely(tvEmotionTrendTitle, "近" + days + "天情绪趋势");
            }
            if (emotionChart != null) {
                renderEmotionTrend(emotionChart, diariesCache, currentRangeDays);
            }
        });
        // 保持 XML 默认选中近30天，无需强制调用 check
    }

    private int moodValue(int moodScore) {
        // Map 1-10 to 1-4 buckets for chart
        if (moodScore <= 0) return 1;
        if (moodScore <= 2) return 1;      // very low
        if (moodScore <= 5) return 2;      // low
        if (moodScore <= 8) return 3;      // medium
        return 4;                          // high
    }

    private void addHotItemView(String title, int count) {
        if (getContext() == null || hotListContainer == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(title + "  ·  完成次数 " + count);
        tv.setTextColor(0xFF333333);
        tv.setTextSize(14);
        tv.setPadding(0, dp(6), 0, dp(6));
        tv.setOnClickListener(v -> {
            // 打开测评搜索页
            Intent intent = new Intent(getContext(), ExercisesSearchActivity.class);
            startActivity(intent);
        });
        hotListContainer.addView(tv);
    }

    private int dp(int v) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (v * scale + 0.5f);
    }

    private void setTextSafely(TextView tv, String text) {
        if (tv == null) return;
        // 保证在主线程更新UI，避免 CalledFromWrongThreadException
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            tv.setText(text);
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (tv != null) tv.setText(text);
            });
        }
    }

    private void initActions(View view) {
        Button btnOpenTestRecords = view.findViewById(R.id.btn_open_test_records);
        Button btnOpenAI = view.findViewById(R.id.btn_open_ai);
        Button btnGoAssessments = view.findViewById(R.id.btn_go_assessments);
        Button btnGoRecords = view.findViewById(R.id.btn_go_records);
        Button btnOpenAuthorization = view.findViewById(R.id.btn_open_authorization);

        if (btnOpenTestRecords != null) {
            btnOpenTestRecords.setOnClickListener(v -> {
                // 在同一页面切换到“测评报告”标签
                ViewPager2 vp = requireActivity().findViewById(R.id.view_pager);
                if (vp != null) vp.setCurrentItem(2, true);
            });
        }
        if (btnGoRecords != null) {
            btnGoRecords.setOnClickListener(v -> {
                // 在同一页面切换到“测评报告”标签
                ViewPager2 vp = requireActivity().findViewById(R.id.view_pager);
                if (vp != null) vp.setCurrentItem(2, true);
            });
        }
        if (btnOpenAI != null) {
            btnOpenAI.setOnClickListener(v -> {
                // 在同一页面切换到“咨询记录”标签
                ViewPager2 vp = requireActivity().findViewById(R.id.view_pager);
                if (vp != null) vp.setCurrentItem(1, true);
            });
        }
        if (btnGoAssessments != null) {
            btnGoAssessments.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ExercisesSearchActivity.class);
                startActivity(intent);
            });
        }
        if (btnOpenAuthorization != null) {
            btnOpenAuthorization.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AuthorizationActivity.class);
                startActivity(intent);
            });
        }
    }
}
