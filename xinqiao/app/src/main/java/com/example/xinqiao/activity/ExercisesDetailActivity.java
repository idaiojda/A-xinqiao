package com.example.xinqiao.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import android.os.Looper;
import androidx.cardview.widget.CardView;
import com.example.xinqiao.util.network.DeepSeekClient;

import com.example.xinqiao.R;
import com.example.xinqiao.bean.QuestionBean;
import java.util.List;
import java.util.Map;
import com.example.xinqiao.bean.ExercisesBean;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.example.xinqiao.bean.TestRecord;
import com.example.xinqiao.dao.TestRecordDao;
import com.example.xinqiao.repository.MedicalRecordRepository;
import com.example.xinqiao.room.entities.TestReportEntity;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.util.scoring.ScoringEngine;

public class ExercisesDetailActivity extends AppCompatActivity {

    private int totalScore;
    private List<QuestionBean> questions;
    private int current = 0;
    private int score = 0;
    private int severitySum = 0;
    private ExercisesBean currentExercisesBean;

    private TextView gradeTextView, questionTextView, progressTextView, progressText, gradeHintTextView;
    private RadioGroup optionsGroup;
    private Button nextButton;
    private ProgressBar progressBar;
    private CardView resultCard, questionCard;

    private DeepSeekClient deepSeekClient;
    private Handler mainHandler;

    private String reportId; // 当前测评记录id
    private int[] userAnswers; // 用户已答选项
    private boolean isResumeFromUnfinished = false;
    private long startTimeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercises_detail_new);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        gradeTextView = findViewById(R.id.gradeTextView);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
        gradeHintTextView = findViewById(R.id.gradeHintTextView);
        resultCard = findViewById(R.id.resultCard);
        questionCard = findViewById(R.id.questionCard);

        // 使用布局中的控件
        questionTextView = findViewById(R.id.questionTextView);
        optionsGroup = findViewById(R.id.optionsGroup);

        // 按钮
        nextButton = findViewById(R.id.nextButton);
        nextButton.setText("下一题");
        nextButton.setTextSize(16);
        nextButton.setTextColor(0xFFFFFFFF);
        nextButton.setPadding(0, 18, 0, 18);

        // 获取题目id
        int id = getIntent().getIntExtra("id", 0);
        // 通过id获取题目列表
        questions = getQuestionsById(id);
        if (questions == null || questions.size() == 0) {
            questionTextView.setText("暂无题目");
            nextButton.setEnabled(false);
            return;
        }
        // 获取未完成测评信息
        reportId = getIntent().getStringExtra("reportId");
        int resumeIndex = getIntent().getIntExtra("currentIndex", 0);
        String answersStr = getIntent().getStringExtra("answers");
        if (reportId != null) {
            isResumeFromUnfinished = true;
        }
        userAnswers = new int[questions.size()];
        for (int i = 0; i < userAnswers.length; i++) userAnswers[i] = -1;
        if (answersStr != null && !answersStr.isEmpty()) {
            String[] arr = answersStr.split(",");
            for (int i = 0; i < arr.length && i < userAnswers.length; i++) {
                try { userAnswers[i] = Integer.parseInt(arr[i]); } catch (Exception ignore) {}
            }
        }
        current = resumeIndex;
        showQuestion(current);

        startTimeMillis = System.currentTimeMillis();

        nextButton.setOnClickListener(v -> {
            int checkedId = optionsGroup.getCheckedRadioButtonId();
            if (checkedId == -1) {
                gradeHintTextView.setText("请选择一个选项");
                gradeHintTextView.setVisibility(View.VISIBLE);
                return;
            }
            gradeHintTextView.setVisibility(View.GONE);
            int selected = optionsGroup.indexOfChild(findViewById(checkedId));
            userAnswers[current] = selected;
            QuestionBean cq = questions.get(current);
            severitySum += severityWeight(cq, selected);
            current++;
            if (current < questions.size()) {
                showQuestion(current);
            } else {
                showReport();
            }
        });

        // 获取当前测评bean（通过id和title）
        int beanId = getIntent().getIntExtra("id", -1);
        String beanTitle = getIntent().getStringExtra("title");
        currentExercisesBean = new ExercisesBean();
        currentExercisesBean.id = beanId;
        currentExercisesBean.title = beanTitle;

        deepSeekClient = new DeepSeekClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 未完成时自动保存进度
        if (current < questions.size()) {
            String userName = AnalysisUtils.readLoginUserName(this);
            TestRecord record = new TestRecord();
            record.title = currentExercisesBean.title;
            record.desc = "未完成测评";
            record.imageResId = currentExercisesBean.background != 0 ? currentExercisesBean.background : com.example.xinqiao.view.ExercisesView.getBackgroundResById(currentExercisesBean.id);
            record.date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            record.status = 0; // 未完成
            record.isFree = true;
            record.reportId = reportId != null ? reportId : ("RPT" + System.currentTimeMillis());
            record.currentIndex = current;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < userAnswers.length; i++) {
                sb.append(userAnswers[i]);
                if (i != userAnswers.length - 1) sb.append(",");
            }
            record.answers = sb.toString();
            TestRecordDao dao = new TestRecordDao(this);
            new Thread(() -> {
                // 直接使用saveTestRecord方法内部的事务机制来处理去重
                dao.saveTestRecord(userName, record);
            }).start();
        }
    }

    private void showQuestion(int index) {
        QuestionBean q = questions.get(index);
        progressText.setText("第 " + (index + 1) + "/" + questions.size() + " 题");
        progressText.setText("第 " + (index + 1) + "/" + questions.size() + " 题");
        
        // 更新进度条
        int progress = (int) (((index + 1) * 100.0) / questions.size());
        progressBar.setProgress(progress);
        
        questionTextView.setText(q.question);
        optionsGroup.removeAllViews();
        for (int i = 0; i < q.options.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(q.options[i]);
            rb.setTextSize(16);
            rb.setTextColor(0xFF111827);
            rb.setButtonDrawable(null);
            rb.setBackgroundResource(R.drawable.bg_option_button_new); // 新样式
            rb.setPadding(48, 32, 48, 32);
            rb.setMinHeight(96); // 增加点击区域
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            rb.setLayoutParams(params);
            optionsGroup.addView(rb);
            if (userAnswers[index] == i) rb.setChecked(true);
        }
        optionsGroup.clearCheck();
        if (userAnswers[index] != -1) {
            ((RadioButton) optionsGroup.getChildAt(userAnswers[index])).setChecked(true);
        }
        gradeHintTextView.setVisibility(View.GONE);
        gradeTextView.setText("");
        if (index == questions.size() - 1) {
            nextButton.setText("提交");
        } else {
            nextButton.setText("下一题");
        }
    }

    private void showReport() {
        // 隐藏问题卡片，显示结果卡片
        questionCard.setVisibility(View.GONE);
        resultCard.setVisibility(View.VISIBLE);

        int maxSeverity = 0;
        if (questions != null) {
            for (QuestionBean q : questions) {
                int m = 3;
                String qt = q.question != null ? q.question : "";
                if (qt.contains("自杀")) m *= 2;
                maxSeverity += m;
            }
        }
        double severityPct = maxSeverity > 0 ? (severitySum * 100.0 / maxSeverity) : 0;
        score = (int) Math.round(severityPct);
        double accuracy = 1.0 - (severityPct / 100.0);
        double completion = 1.0;
        double consistency = 0.9;
        double activity = isResumeFromUnfinished ? 0.7 : 0.85;
        double expectedMinutes = Math.max(2, (questions != null ? questions.size() : 5) * 0.5);
        double spentMinutes = Math.max(0.1, (System.currentTimeMillis() - startTimeMillis) / 60000.0);
        double timeliness = Math.min(1.0, expectedMinutes / spentMinutes);
        double verification = 0.5;

        ScoringEngine.Factors f = new ScoringEngine.Factors();
        f.behaviorActivity = activity;
        f.behaviorCompletion = completion;
        f.behaviorConsistency = consistency;
        f.qualityAccuracy = accuracy;
        f.qualityTimeliness = timeliness;
        f.objectiveVerification = verification;

        ScoringEngine.Result sr = ScoringEngine.evaluate(f, System.currentTimeMillis(), System.currentTimeMillis());

        String riskLevel = (score >= 80) ? "高风险" : (score >= 60 ? "中风险" : "低风险");
        gradeTextView.setText("风险等级：" + riskLevel + "\n" + getAdvice(score));
        gradeTextView.setTextSize(22);
        gradeTextView.setPadding(36, 36, 36, 36);
        gradeTextView.setGravity(android.view.Gravity.CENTER);
        
        // 美化分析区块
        TextView analysisTextView = findViewById(R.id.analysisTextView);
        analysisTextView.setTextSize(16);
        analysisTextView.setPadding(32, 32, 32, 32);
        analysisTextView.setGravity(android.view.Gravity.CENTER);
        String localAnalysis = generateLocalAnalysis(riskLevel);
        analysisTextView.setText(localAnalysis);
        
        // --- 动画效果 ---
        Animation gradeAnim = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        gradeTextView.startAnimation(gradeAnim);
        Animation analysisAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        analysisTextView.startAnimation(analysisAnim);
        
        // 动态分析
        if (currentExercisesBean != null) {
            int total = questions != null ? questions.size() * 20 : 100;
            String base = localAnalysis + "\n\n" + "正在联网分析，请稍候...";
            analysisTextView.setText(base);
            StringBuilder sb = new StringBuilder();
            sb.append("测评名称：").append(currentExercisesBean.title).append("\n");
            sb.append("答题情况：\n");
            if (questions != null) {
                for (int i = 0; i < questions.size(); i++) {
                    sb.append(i + 1).append(". ").append(questions.get(i).question).append("\n");
                    sb.append("选项：");
                    for (int j = 0; j < questions.get(i).options.length; j++) {
                        sb.append((char)('A' + j)).append(".").append(questions.get(i).options[j]).append("  ");
                    }
                    sb.append("\n");
                    sb.append("用户选择：");
                    int checkedId = optionsGroup.getCheckedRadioButtonId();
                    sb.append("（已提交）\n");
                }
            }
            sb.append("请根据以上测评内容和分数，给出专业、温暖、共情的心理分析建议。");
            String prompt = sb.toString();
            deepSeekClient.sendMessage(prompt, new DeepSeekClient.ChatCallback() {
                @Override
                public void onSuccess(String response) {
                    mainHandler.post(() -> {
                        StringBuilder esb = new StringBuilder();
                        esb.append(localAnalysis).append("\n\n");
                        esb.append("[联网补充]\n");
                        esb.append(response).append("\n\n");
                        esb.append("评分说明: ");
                        esb.append("基础=").append(sr.baseScore).append(", 加权=").append(sr.weightedScore).append(", 动态=").append(sr.adjustedScore).append("\n");
                        for (Map.Entry<String, Double> e : sr.breakdown.entrySet()) {
                            esb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                        analysisTextView.setText(esb.toString());
                        // 分析结果出现时再加一次淡入动画
                        Animation fadeIn = AnimationUtils.loadAnimation(ExercisesDetailActivity.this, R.anim.fade_in);
                        analysisTextView.startAnimation(fadeIn);
                    });
                }
                @Override
                public void onFailure(String error) {
                    mainHandler.post(() -> {
                        String analysis = currentExercisesBean.getAnalysis(score, total);
                        StringBuilder esb = new StringBuilder();
                        esb.append(localAnalysis).append("\n\n");
                        esb.append("[本地分析] ").append(analysis).append("\n[联网失败] ").append(error).append("\n\n");
                        esb.append("评分说明: ");
                        esb.append("基础=").append(sr.baseScore).append(", 加权=").append(sr.weightedScore).append(", 动态=").append(sr.adjustedScore).append("\n");
                        for (Map.Entry<String, Double> e : sr.breakdown.entrySet()) {
                            esb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
                        }
                        analysisTextView.setText(esb.toString());
                        Toast.makeText(ExercisesDetailActivity.this, "联网分析失败，已回退本地分析", Toast.LENGTH_SHORT).show();
                        Animation fadeIn = AnimationUtils.loadAnimation(ExercisesDetailActivity.this, R.anim.fade_in);
                        analysisTextView.startAnimation(fadeIn);
                    });
                }
            });
        } else {
            analysisTextView.setText("暂无分析");
        }
        // === 自动补全：测评完成写入数据库 ===
        String userName = AnalysisUtils.readLoginUserName(this);
        TestRecord record = new TestRecord();
        record.title = currentExercisesBean.title;
        record.desc = getAdvice(score); // 可自定义为测评简述
        record.imageResId = currentExercisesBean.background != 0 ? currentExercisesBean.background : com.example.xinqiao.view.ExercisesView.getBackgroundResById(currentExercisesBean.id);
        record.date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        
        // 判断测评是否需要付费
        boolean needPayment = currentExercisesBean.id > 5; // 假设id大于5的测评需要付费
        if (needPayment) {
            record.status = 2; // 待支付
            record.isFree = false;
        } else {
            record.status = 1; // 已完成
            record.isFree = true;
        }
        
        record.reportId = reportId != null ? reportId : ("RPT" + System.currentTimeMillis());
        record.currentIndex = questions.size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userAnswers.length; i++) {
            sb.append(userAnswers[i]);
            if (i != userAnswers.length - 1) sb.append(",");
        }
        record.answers = sb.toString();
        TestRecordDao dao = new TestRecordDao(this);
        new Thread(() -> {
            // 直接使用saveTestRecord方法内部的事务机制来处理去重
            dao.saveTestRecord(userName, record);
            
            // 如果为免费测评，直接生成并保存报告
            if (!needPayment) {
                try {
                    MedicalRecordRepository repo = new MedicalRecordRepository(ExercisesDetailActivity.this);
                    TestReportEntity entity = new TestReportEntity();
                    entity.userName = userName != null ? userName : "";
                    entity.reportId = record.reportId;
                    entity.type = currentExercisesBean != null ? currentExercisesBean.title : "心理测评";
                    entity.score = score;
                    entity.riskLevel = (score >= 80) ? "高风险" : (score >= 60 ? "中风险" : "低风险");
                    entity.date = record.date;
                    String detailsPlain = "测评名称: " + entity.type +
                            "\n分数: " + score +
                            "\n等级: " + getGrade(score) +
                            "\n建议: " + getAdvice(score) +
                            "\n报告生成于: " + entity.date +
                            "\n报告ID: " + entity.reportId;
                    repo.addTestReport(entity, detailsPlain);
                } catch (Exception ignore) {}
            }

            // 如果是待支付状态，提示用户
            if (needPayment) {
                mainHandler.post(() -> {
                    Toast.makeText(ExercisesDetailActivity.this, "测评完成，查看完整报告需要支付", Toast.LENGTH_LONG).show();
                    // 可以选择直接跳转到支付页面
                    Intent intent = new Intent(ExercisesDetailActivity.this, TestReportActivity.class);
                    intent.putExtra("reportId", record.reportId);
                    startActivity(intent);
                });
            }
        }).start();
        // === END ===
    }

    private String getGrade(int score) {
        if (score >= 80 && score <= 100) {
            return "A";
        } else if (score >= 60 && score < 80) {
            return "B";
        } else if (score >= 0 && score < 60) {
            return "C";
        } else {
            return "无效分数";
        }
    }

    private String getAdvice(int score) {
        if (score >= 90) {
            return "您的焦虑水平非常高，建议尽快寻求专业心理咨询师的帮助，及时进行心理疏导和干预。";
        } else if (score >= 80) {
            return "您的焦虑水平较高，建议关注自身情绪变化，尝试放松训练、正念冥想等方法，并考虑寻求心理支持。";
        } else if (score >= 70) {
            return "您有一定的焦虑倾向，建议加强自我调节，保持规律作息，适当运动，必要时与亲友沟通。";
        } else if (score >= 60) {
            return "您的焦虑水平略高于平均，建议关注压力源，适当放松，保持积极心态。";
        } else if (score >= 40) {
            return "您的焦虑水平处于正常范围，建议继续保持良好的生活习惯和心态。";
        } else {
            return "您的焦虑水平较低，心理状态良好，请继续保持积极健康的生活方式。";
        }
    }

    private int severityWeight(QuestionBean q, int index) {
        String qt = q.question != null ? q.question : "";
        String[] opts = q.options != null ? q.options : new String[0];
        int base;
        if (opts.length >= 4) {
            String first = opts[0];
            if (containsAny(first, new String[]{"没有","从不","很好","非常满意","非常明确","经常"})) {
                base = index;
            } else {
                base = index;
            }
        } else {
            base = index;
        }
        if (qt.contains("自杀")) base *= 2;
        return base;
    }

    private boolean containsAny(String s, String[] arr) {
        if (s == null) return false;
        for (String a : arr) { if (s.contains(a)) return true; }
        return false;
    }

    private String generateLocalAnalysis(String riskLevel) {
        boolean selfHarm = false;
        boolean lowMood = false;
        boolean interestLoss = false;
        boolean fatigue = false;
        boolean poorSleep = false;
        for (int i = 0; i < questions.size(); i++) {
            QuestionBean q = questions.get(i);
            int ans = userAnswers[i];
            if (ans < 0) continue;
            String qt = q.question != null ? q.question : "";
            if (qt.contains("自杀")) selfHarm = ans > 0;
            if (qt.contains("低落")) lowMood = ans >= 2;
            if (qt.contains("兴趣")) interestLoss = ans >= 2;
            if (qt.contains("疲惫") || qt.contains("无力")) fatigue = ans >= 2;
            if (qt.contains("睡眠")) poorSleep = ans >= 2 || (q.options.length >= 4 && ans >= 2);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("综合评估：").append(riskLevel).append("\n");
        sb.append(selfHarm ? "存在自杀风险，需要尽快寻求专业帮助。" : "未报告自杀念头。").append("\n");
        if (lowMood) sb.append("存在明显低落情绪。\n");
        if (interestLoss) sb.append("对原有兴趣的投入下降。\n");
        if (fatigue) sb.append("存在疲惫或精力不足表现。\n");
        if (poorSleep) sb.append("睡眠质量存在问题。\n");
        if (!(lowMood || interestLoss || fatigue || poorSleep)) sb.append("总体状态稳定，建议继续保持良好作息与情绪管理。\n");
        sb.append("建议：保持规律作息、适度运动与情绪记录；若持续两周以上或影响日常，请考虑专业咨询。");
        return sb.toString();
    }

    // 静态方法：通过id获取题目列表（可根据实际情况改为数据库查询）
    public static List<QuestionBean> getQuestionsById(int id) {
        // 这里用静态模拟，实际可用数据库或单例缓存
        List<QuestionBean> questions = new java.util.ArrayList<>();
        switch (id) {
            case 1:
                questions.add(new QuestionBean("你在恋爱中更看重什么？", new String[]{"安全感", "激情", "陪伴", "成长"}, 0));
                questions.add(new QuestionBean("遇到分歧时你通常会？", new String[]{"主动沟通", "冷处理", "顺其自然", "寻求朋友帮助"}, 0));
                questions.add(new QuestionBean("你对恋人最大的期待是？", new String[]{"理解和包容", "浪漫惊喜", "共同进步", "经济支持"}, 0));
                questions.add(new QuestionBean("你是否容易因小事和恋人争吵？", new String[]{"很少", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你会主动表达自己的情感吗？", new String[]{"经常", "偶尔", "很少", "几乎不"}, 0));
                questions.add(new QuestionBean("你认为恋爱中最重要的品质是？", new String[]{"信任", "忠诚", "独立", "包容"}, 0));
                break;
            case 2:
                questions.add(new QuestionBean("你在陌生人面前说话会感到紧张吗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否害怕在公共场合被注视？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你会因为害怕尴尬而避免社交活动吗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你在小组讨论时会主动发言吗？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
                questions.add(new QuestionBean("你是否担心自己的表现会被别人评价？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                break;
            case 3:
                questions.add(new QuestionBean("你是否容易和陌生人搭话？", new String[]{"很容易", "一般", "有点难", "很难"}, 0));
                questions.add(new QuestionBean("你有几个可以倾诉的朋友？", new String[]{"很多", "几个", "很少", "没有"}, 0));
                questions.add(new QuestionBean("你会主动组织聚会或活动吗？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
                questions.add(new QuestionBean("遇到矛盾时你会如何处理？", new String[]{"主动沟通", "回避", "冷战", "求助他人"}, 0));
                break;
            case 4:
                questions.add(new QuestionBean("你最近是否经常感到情绪低落？", new String[]{"没有", "轻度", "中度", "重度"}, 0));
                questions.add(new QuestionBean("你是否对平时感兴趣的事物失去兴趣？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否经常感到疲惫或没有精力？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你的睡眠质量如何？", new String[]{"很好", "一般", "较差", "很差"}, 0));
                questions.add(new QuestionBean("你是否有自责或无价值感？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否有自杀念头？", new String[]{"没有", "偶尔", "经常", "总是"}, 0));
                break;
            case 5:
                questions.add(new QuestionBean("你是否经常感到紧张或坐立不安？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否容易为小事担忧？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否经常感到心慌或出汗？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否会因为担心而影响睡眠？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否觉得难以控制自己的焦虑情绪？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                break;
            case 6:
                questions.add(new QuestionBean("你是否经常感到情绪低落？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否对生活失去兴趣？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否经常感到疲惫或无力？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否经常自责或觉得自己没用？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否有过自杀的念头？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                break;
            case 7:
                questions.add(new QuestionBean("你是否经常感到压力大？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否因压力影响睡眠？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否因压力而情绪波动？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否有缓解压力的有效方法？", new String[]{"有", "偶尔有", "很少有", "没有"}, 0));
                break;
            case 8:
                questions.add(new QuestionBean("你遇到挫折时会？", new String[]{"自我安慰", "寻求帮助", "消极回避", "积极面对"}, 0));
                questions.add(new QuestionBean("你倾向于如何调节情绪？", new String[]{"运动", "倾诉", "独处", "娱乐"}, 0));
                questions.add(new QuestionBean("你是否会主动寻求心理咨询？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
                questions.add(new QuestionBean("你是否会写日记或记录情绪？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
                questions.add(new QuestionBean("你是否愿意与亲友分享自己的困扰？", new String[]{"非常愿意", "偶尔", "很少", "不愿意"}, 0));
                break;
            case 9:
                questions.add(new QuestionBean("你是否害怕与人建立亲密关系？", new String[]{"从不", "偶尔", "经常", "总是"}, 0));
                questions.add(new QuestionBean("你是否习惯独处？", new String[]{"非常习惯", "偶尔", "不太习惯", "不习惯"}, 0));
                questions.add(new QuestionBean("你是否会主动疏远亲近的人？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
                questions.add(new QuestionBean("你是否觉得依赖别人会让你不安？", new String[]{"非常不安", "有点不安", "无所谓", "不会"}, 0));
                break;
            case 10:
                questions.add(new QuestionBean("你对目前的生活满意吗？", new String[]{"非常满意", "比较满意", "一般", "不满意"}, 0));
                questions.add(new QuestionBean("你是否有明确的人生目标？", new String[]{"非常明确", "比较明确", "不太明确", "没有"}, 0));
                questions.add(new QuestionBean("你是否经常感到快乐？", new String[]{"经常", "偶尔", "很少", "从不"}, 0));
                questions.add(new QuestionBean("你是否有良好的人际关系？", new String[]{"非常好", "一般", "较差", "很差"}, 0));
                questions.add(new QuestionBean("你是否有健康的生活方式？", new String[]{"非常健康", "比较健康", "一般", "不健康"}, 0));
                break;
        }
        return questions;
    }
}
