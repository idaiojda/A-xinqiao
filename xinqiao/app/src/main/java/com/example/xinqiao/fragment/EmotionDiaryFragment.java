package com.example.xinqiao.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;

import com.example.xinqiao.R;
import com.example.xinqiao.adapter.EmotionDiaryAdapter;
import com.example.xinqiao.bean.EmotionEntry;
import com.example.xinqiao.util.RecyclerViewOptimizer;
import com.example.xinqiao.repository.MedicalRecordRepository;
import com.example.xinqiao.room.entities.EmotionDiaryEntity;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.util.CryptoUtil;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.MPPointF;
import com.example.xinqiao.widget.SimpleEmotionMarkerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmotionDiaryFragment extends Fragment {
    private RecyclerView rvList;
    private Button btnAdd;
    private Button btnFilter;
    private TextView tvStartDate, tvEndDate;
    private EmotionDiaryAdapter adapter;
    private List<EmotionEntry> entries = new ArrayList<>();
    private MedicalRecordRepository repo;
    private String userName;
    private LineChart emotionChart;
    private TextView tvChartTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_emotion_diary, container, false);
        rvList = view.findViewById(R.id.rv_emotion_diary);
        emotionChart = view.findViewById(R.id.line_emotion_diary);
        tvChartTitle = view.findViewById(R.id.tv_emotion_chart_title);
        btnAdd = view.findViewById(R.id.btn_add_emotion);
        btnFilter = view.findViewById(R.id.btn_filter);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EmotionDiaryAdapter(entries);
        rvList.setAdapter(adapter);
        RecyclerViewOptimizer.optimizeDefault(rvList);
        rvList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fade_in));

        // 鍒濆鍖栦粨搴撲笌鐢ㄦ埛鍚嶏紝骞朵粠Room鍔犺浇鏁版嵁
        repo = new MedicalRecordRepository(requireContext());
        userName = AnalysisUtils.readLoginUserName(requireContext());
        initDefaultDates();
        refreshFromRepo();

        // 鐐瑰嚮鏌ョ湅璇︽儏銆侀暱鎸夌紪杈?鍒犻櫎
        adapter.setOnItemClickListener((position, entry) -> showViewDialog(inflater, position, entry));
        adapter.setOnItemLongClickListener((position, entry) -> showActionDialog(inflater, position, entry));

        btnAdd.setOnClickListener(v -> showAddDialog(inflater));
        btnFilter.setOnClickListener(v -> doFilter());
        tvStartDate.setOnClickListener(v -> pickDate(tvStartDate));
        tvEndDate.setOnClickListener(v -> pickDate(tvEndDate));
        return view;
    }

    private void showAddDialog(LayoutInflater inflater) {
        View dialogView = inflater.inflate(R.layout.dialog_emotion_entry, null);
        SeekBar sbMood = dialogView.findViewById(R.id.sb_mood);
        TextView tvMoodValue = dialogView.findViewById(R.id.tv_mood_value);
        EditText etNote = dialogView.findViewById(R.id.et_note);

        tvMoodValue.setText("\u5FC3\u60C5\uFF1A" + sbMood.getProgress());
        sbMood.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMoodValue.setText("\u5FC3\u60C5\uFF1A" + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 鍔ㄦ€佺編鍖栧績鎯呰壊鍧楅鑹?        styleMoodChip(tvMoodValue, sbMood.getProgress());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("\u65B0\u589E\u60C5\u7EEA\u8BB0\u5F55")
                .setView(dialogView)
                .setPositiveButton("淇濆瓨", (d, which) -> {
                    String note = etNote.getText() != null ? etNote.getText().toString() : "";
                    String day = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    repo.addEmotionDiaryAsync(
                            userName != null ? userName : "",
                            day,
                            sbMood.getProgress(),
                            note,
                            new MedicalRecordRepository.EmotionDiaryIdCallback() {
                                @Override
                                public void onSuccess(long id) {
                                    requireActivity().runOnUiThread(EmotionDiaryFragment.this::refreshFromRepo);
                                }
                                @Override
                                public void onError(Exception e) {
                                    requireActivity().runOnUiThread(EmotionDiaryFragment.this::refreshFromRepo);
                                }
                            }
                    );
                })
                .setNegativeButton("\u53D6\u6D88", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            // 瀵硅瘽妗嗘寜閽潃鑹?            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#2563EB"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6B7280"));
        });
        dialog.show();
    }

    private void showActionDialog(LayoutInflater inflater, int position, EmotionEntry entry) {
        String[] actions = new String[]{"\u7F16\u8F91", "\u5220\u9664"};
        new AlertDialog.Builder(requireContext())
                .setTitle("\u64CD\u4F5C")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(inflater, position, entry);
                    } else if (which == 1) {
                        repo.deleteEmotionDiaryByIdAsync(
                                entry.id,
                                userName != null ? userName : "",
                                new MedicalRecordRepository.EmotionDiaryRowsCallback() {
                                    @Override
                                    public void onSuccess(int rows) {
                                        requireActivity().runOnUiThread(EmotionDiaryFragment.this::refreshFromRepo);
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        requireActivity().runOnUiThread(EmotionDiaryFragment.this::refreshFromRepo);
                                    }
                                }
                        );
                    }
                })
                .show();
    }

    private void showEditDialog(LayoutInflater inflater, int position, EmotionEntry entry) {
        View dialogView = inflater.inflate(R.layout.dialog_emotion_entry, null);
        SeekBar sbMood = dialogView.findViewById(R.id.sb_mood);
        TextView tvMoodValue = dialogView.findViewById(R.id.tv_mood_value);
        EditText etNote = dialogView.findViewById(R.id.et_note);

        sbMood.setProgress(entry.mood);
        tvMoodValue.setText("\u5FC3\u60C5\uFF1A" + sbMood.getProgress());
        etNote.setText(entry.note != null ? entry.note : "");

        sbMood.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMoodValue.setText("\u5FC3\u60C5\uFF1A" + progress);
                styleMoodChip(tvMoodValue, progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        styleMoodChip(tvMoodValue, sbMood.getProgress());

        MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("\u7F16\u8F91\u60C5\u7EEA\u8BB0\u5F55")
                .setView(dialogView)
                .setPositiveButton("淇濆瓨", (d, which) -> {
                    int mood = sbMood.getProgress();
                    String note = etNote.getText() != null ? etNote.getText().toString() : "";
                    // Room涓娇鐢▂yyy-MM-dd淇濆瓨鏃ユ湡
                    String day = entry.date != null && entry.date.length() >= 10 ? entry.date.substring(0, 10) : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    new Thread(() -> {
                        repo.updateEmotionDiary(entry.id, userName != null ? userName : "", day, mood, note);
                        requireActivity().runOnUiThread(this::refreshFromRepo);
                    }).start();
                })
                .setNegativeButton("\u53D6\u6D88", null);
        AlertDialog dialog2 = builder2.create();
        dialog2.setOnShowListener(di -> {
            dialog2.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#2563EB"));
            dialog2.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6B7280"));
        });
        dialog2.show();
    }

    private void styleMoodChip(TextView tv, int mood) {
        String face;
        int fillColor;
        if (mood <= 3) {
            face = " \uD83D\uDE14"; // pensive face
            fillColor = Color.parseColor("#DBEAFE");
        } else if (mood <= 6) {
            face = " \uD83D\uDE10"; // neutral face
            fillColor = Color.parseColor("#DCFCE7");
        } else if (mood <= 8) {
            face = " \uD83D\uDE42"; // slightly smiling face
            fillColor = Color.parseColor("#FFEDD5");
        } else {
            face = " \uD83D\uDE04"; // grinning face with smiling eyes
            fillColor = Color.parseColor("#FEE2E2");
        }
        tv.setText("\u5FC3\u60C5\uFF1A" + mood + face);
        try {
            android.graphics.drawable.Drawable bg = tv.getBackground();
            if (bg instanceof GradientDrawable) {
                GradientDrawable gd = (GradientDrawable) bg.mutate();
                gd.setColor(fillColor);
            }
        } catch (Exception ignore) {}
    }

    private void refreshFromRepo() {
        repo.getEmotionDiariesAsync(userName != null ? userName : "", false, new MedicalRecordRepository.EmotionDiariesCallback() {
            @Override
            public void onSuccess(List<EmotionDiaryEntity> list) {
                requireActivity().runOnUiThread(() -> applyRepoData(list));
            }
            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> applyRepoData(new java.util.ArrayList<>()));
            }
        });
    }

    private void doFilter() {
        String start = tvStartDate.getText() != null ? tvStartDate.getText().toString().trim() : "";
        String end = tvEndDate.getText() != null ? tvEndDate.getText().toString().trim() : "";
        if (start.isEmpty() || end.isEmpty()) {
            refreshFromRepo();
            return;
        }
        repo.getEmotionDiariesByDateRangeAsync(userName != null ? userName : "", start, end, new MedicalRecordRepository.EmotionDiariesCallback() {
            @Override
            public void onSuccess(List<EmotionDiaryEntity> list) {
                requireActivity().runOnUiThread(() -> applyRepoData(list));
            }
            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> applyRepoData(new java.util.ArrayList<>()));
            }
        });
    }

    private void applyRepoData(List<EmotionDiaryEntity> list) {
        List<EmotionEntry> ui = new ArrayList<>();
        SimpleDateFormat showFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (EmotionDiaryEntity e : list) {
            EmotionEntry item = new EmotionEntry();
            item.id = e.id;
            item.date = e.date != null ? e.date : showFmt.format(new Date());
            item.mood = e.mood;
            item.note = CryptoUtil.decrypt(e.noteEncrypted);
            ui.add(item);
        }
        entries = ui;
        adapter.setData(entries);
        rvList.scheduleLayoutAnimation();
        renderEmotionChart(list);
    }

    private void initDefaultDates() {
        String end = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long sevenDays = 6L * 24L * 60L * 60L * 1000L;
        String start = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(System.currentTimeMillis() - sevenDays));
        tvStartDate.setText(start);
        tvEndDate.setText(end);
    }

    private void pickDate(TextView target) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String mm = String.format(Locale.getDefault(), "%02d", month + 1);
            String dd = String.format(Locale.getDefault(), "%02d", dayOfMonth);
            target.setText(year + "-" + mm + "-" + dd);
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void renderEmotionChart(List<EmotionDiaryEntity> list) {
        if (emotionChart == null) return;
        emotionChart.getDescription().setEnabled(false);
        emotionChart.setNoDataText("\u6682\u65E0\u60C5\u7EEA\u6570\u636E");
        emotionChart.setDrawGridBackground(false);
        emotionChart.setPinchZoom(false);

        // Aggregate daily averages and keep first note of the day
        java.util.Map<String, Float> daySum = new java.util.HashMap<>();
        java.util.Map<String, Integer> dayCount = new java.util.HashMap<>();
        java.util.Map<String, String> dayNote = new java.util.HashMap<>();
        if (list != null) {
            for (EmotionDiaryEntity e : list) {
                String day = e.date; // yyyy-MM-dd
                Float s = daySum.get(day);
                Integer c = dayCount.get(day);
                daySum.put(day, (s == null ? 0f : s) + moodValue(e.mood));
                dayCount.put(day, (c == null ? 0 : c) + 1);
                if (!dayNote.containsKey(day)) {
                    String note = CryptoUtil.decrypt(e.noteEncrypted);
                    if (note != null && !note.isEmpty()) {
                        dayNote.put(day, note);
                    }
                }
            }
        }

        // 鐢熸垚鎸夋棩鏈熸帓搴忕殑鏍囩鍒楄〃
        java.util.List<String> labels = new java.util.ArrayList<>(daySum.keySet());
        java.util.Collections.sort(labels);
        java.util.List<Entry> points = new java.util.ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String d = labels.get(i);
            float avg = 0f;
            Float s = daySum.get(d);
            Integer c = dayCount.get(d);
            if (s != null && c != null && c > 0) avg = s / c;
            if (avg < 0f) avg = 0f; if (avg > 10f) avg = 10f;
            points.add(new Entry(i, avg));
        }

        LineDataSet set = new LineDataSet(points, "\u60C5\u7EEA\u5F3A\u5EA6");
        set.setColor(android.graphics.Color.parseColor("#F59E0B"));
        set.setLineWidth(2f);
        set.setCircleColor(android.graphics.Color.parseColor("#F59E0B"));
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setDrawHighlightIndicators(true);
        set.setHighLightColor(android.graphics.Color.parseColor("#FB923C"));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(android.graphics.Color.parseColor("#FFF7ED"));

        LineData data = new LineData(set);
        emotionChart.setData(data);

        XAxis x = emotionChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        x.setGridColor(android.graphics.Color.parseColor("#F3F4F6"));
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setLabelRotationAngle(0f);

        YAxis left = emotionChart.getAxisLeft();
        left.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        left.setGridColor(android.graphics.Color.parseColor("#F3F4F6"));
        left.setAxisMinimum(0f);
        left.setAxisMaximum(10f);
        emotionChart.getAxisRight().setEnabled(false);

        // Marker: show date, record count and note
        SimpleEmotionMarkerView marker = new SimpleEmotionMarkerView(requireContext(), labels, dayCount, dayNote);
        emotionChart.setMarker(marker);
        if (tvChartTitle != null) {
            String start = tvStartDate.getText() != null ? tvStartDate.getText().toString().trim() : "";
            String end = tvEndDate.getText() != null ? tvEndDate.getText().toString().trim() : "";
            if (!start.isEmpty() && !end.isEmpty()) {
                tvChartTitle.setText("\u60C5\u7EEA\u8D8B\u52BF\uFF08" + start + " \u81F3 " + end + "\uFF09");
            }
        }
        emotionChart.animateX(700);
        emotionChart.invalidate();
    }

    private int moodValue(int mood) {
        // 鍏煎0-10涓?-10涓ょ杈撳叆
        if (mood < 0) mood = 0; if (mood > 10) mood = 10;
        return mood;
    }

            private void showViewDialog(LayoutInflater inflater, int position, EmotionEntry entry) {
        View dialogView = inflater.inflate(R.layout.dialog_emotion_entry, null);
        SeekBar sbMood = dialogView.findViewById(R.id.sb_mood);
        TextView tvMoodValue = dialogView.findViewById(R.id.tv_mood_value);
        EditText etNote = dialogView.findViewById(R.id.et_note);

        sbMood.setProgress(entry.mood);
        sbMood.setEnabled(false);
        tvMoodValue.setText("\u5FC3\u60C5\uFF1A" + sbMood.getProgress());
        styleMoodChip(tvMoodValue, sbMood.getProgress());

        etNote.setText(entry.note != null ? entry.note : "");
        etNote.setEnabled(false);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(entry.date != null ? entry.date : "")
                .setView(dialogView)
                .setPositiveButton("\u5173\u95ED", null)
                .setNeutralButton("\u7F16\u8F91", (d, which) -> showEditDialog(inflater, position, entry))
                .show();
    }
}




