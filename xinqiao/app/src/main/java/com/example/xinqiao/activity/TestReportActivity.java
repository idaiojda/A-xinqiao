package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.xinqiao.R;
import com.example.xinqiao.dao.TestRecordDao;
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.utils.PaymentUtils;
import com.example.xinqiao.repository.MedicalRecordRepository;
import com.example.xinqiao.room.entities.TestReportEntity;
import com.example.xinqiao.util.CryptoUtil;

public class TestReportActivity extends AppCompatActivity {
    private String reportId;
    private TextView tvReportContent;
    private Button btnPay;
    private TestRecordDao testRecordDao;
    private PaymentUtils paymentUtils;
    private String userName;
    private MedicalRecordRepository repo;
    private static final double REPORT_PRICE = 9.9; // 报告价格
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_report);
        
        // 初始化组件
        tvReportContent = findViewById(R.id.tv_report_content);
        btnPay = findViewById(R.id.btn_pay);
        
        // 获取数据
        reportId = getIntent().getStringExtra("reportId");
        userName = AnalysisUtils.readLoginUserName(this);
        testRecordDao = new TestRecordDao(this);
        paymentUtils = new PaymentUtils(this);
        repo = new MedicalRecordRepository(this);
        
        // 加载报告内容
        loadReportContent();
        
        // 检查报告状态并设置UI
        checkReportStatus();
        
        // 设置支付按钮点击事件
        btnPay.setOnClickListener(v -> handlePayment());
    }
    
    /**
     * 加载报告内容
     */
    private void loadReportContent() {
        // 优先从已保存的Room报告中加载
        repo.getTestReportByReportIdAsync(userName != null ? userName : "", reportId, new MedicalRecordRepository.TestReportEntityCallback() {
            @Override
            public void onSuccess(TestReportEntity entity) {
                runOnUiThread(() -> {
                    if (entity != null) {
                        String details = entity.detailsEncrypted != null ? CryptoUtil.decrypt(entity.detailsEncrypted) : "";
                        String text = (details != null && !details.isEmpty()) ? details : ("报告ID: " + reportId + "\n\n完整报告内容...\n\n感谢您的支持！");
                        tvReportContent.setText(text);
                    } else {
                        // 兜底：若按用户名查询不到，尝试仅按 reportId 查询
                        repo.getTestReportByReportIdAnyUserAsync(reportId, new MedicalRecordRepository.TestReportEntityCallback() {
                            @Override
                            public void onSuccess(TestReportEntity anyUserEntity) {
                                runOnUiThread(() -> {
                                    if (anyUserEntity != null) {
                                        String details2 = anyUserEntity.detailsEncrypted != null ? CryptoUtil.decrypt(anyUserEntity.detailsEncrypted) : "";
                                        String text2 = (details2 != null && !details2.isEmpty()) ? details2 : ("报告ID: " + reportId + "\n\n完整报告内容...\n\n感谢您的支持！");
                                        tvReportContent.setText(text2);
                                    } else {
                                        // 两次查询都未找到时，若记录已完成则自动生成报告
                                        attemptAutoGenerateFromRecord();
                                    }
                                });
                            }
                            @Override
                            public void onError(Exception e2) {
                                attemptAutoGenerateFromRecord();
                            }
                        });
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                attemptAutoGenerateFromRecord();
            }
        });
    }

    /**
     * 当报告查询不到且记录状态为已完成(1)时，自动生成并保存报告，然后展示。
     */
    private void attemptAutoGenerateFromRecord() {
        new Thread(() -> {
            int status = testRecordDao.getTestRecordStatus(reportId);
            if (status == 1) {
                try {
                    com.example.xinqiao.bean.TestRecord r = testRecordDao.getTestRecordById(reportId);
                    TestReportEntity entity = new TestReportEntity();
                    entity.userName = userName != null ? userName : "";
                    entity.reportId = reportId;
                    entity.type = (r != null && r.title != null) ? r.title : "心理测评";
                    int answered = (r != null) ? r.currentIndex : 0;
                    entity.score = answered * 20;
                    entity.riskLevel = (entity.score >= 80) ? "高风险" : (entity.score >= 60 ? "中风险" : "低风险");
                    entity.date = (r != null && r.date != null) ? r.date : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                    String detailsPlain = "报告ID: " + reportId +
                            "\n测评名称: " + entity.type +
                            "\n分数: " + entity.score +
                            "\n风险等级: " + entity.riskLevel +
                            "\n摘要: " + ((r != null && r.desc != null) ? r.desc : "已完成测评，自动生成报告") +
                            "\n生成时间: " + entity.date;
                    repo.addTestReport(entity, detailsPlain);
                    runOnUiThread(() -> tvReportContent.setText(detailsPlain));
                } catch (Exception ignore) {
                    runOnUiThread(() -> tvReportContent.setText("报告ID: " + reportId + "\n这里展示测评报告详情内容...\n\n完整报告需要支付才能查看。"));
                }
            } else if (status == 2) {
                runOnUiThread(() -> tvReportContent.setText("报告ID: " + reportId + "\n这里展示测评报告详情内容...\n\n完整报告需要支付才能查看。"));
            } else {
                runOnUiThread(() -> tvReportContent.setText("报告ID: " + reportId + "\n这里展示测评报告详情内容...\n\n完整报告需要支付才能查看。"));
            }
        }).start();
    }
    
    /**
     * 检查报告状态并设置UI
     */
    private void checkReportStatus() {
        new Thread(() -> {
            // 查询记录状态
            int status = testRecordDao.getTestRecordStatus(reportId);
            
            // 在UI线程更新界面
            runOnUiThread(() -> {
                if (status == 1) { // 已完成
                    // 已完成时只隐藏支付按钮，内容由 loadReportContent 负责展示
                    btnPay.setVisibility(View.GONE);
                } else if (status == 2) { // 待支付
                    btnPay.setVisibility(View.VISIBLE);
                    btnPay.setText("支付 ¥" + REPORT_PRICE);
                } else { // 其他状态
                    btnPay.setVisibility(View.GONE);
                }
            });
        }).start();
    }
    
    /**
     * 处理支付逻辑
     */
    private void handlePayment() {
        // 检查余额
        paymentUtils.getBalance(userName, new PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                double balance = paymentUtils.getCurrentBalance();
                if (balance < REPORT_PRICE) {
                    // 余额不足，提示充值
                    Toast.makeText(TestReportActivity.this, "余额不足，请先充值", Toast.LENGTH_SHORT).show();
                    // 跳转到充值页面
                    android.content.Intent intent = new android.content.Intent(TestReportActivity.this, RechargeActivity.class);
                    startActivity(intent);
                } else {
                    // 执行支付
                    processPayment();
                }
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(TestReportActivity.this, "获取余额失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 执行支付流程
     */
    private void processPayment() {
        // 扣除余额
        paymentUtils.deductBalance(userName, REPORT_PRICE, new PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                // 支付成功，更新记录状态
                new Thread(() -> {
                    boolean updated = testRecordDao.updateTestRecordStatus(reportId, 1); // 更新为已完成状态
                    runOnUiThread(() -> {
                        if (updated) {
                            Toast.makeText(TestReportActivity.this, "支付成功", Toast.LENGTH_SHORT).show();
                            btnPay.setVisibility(View.GONE);
                            // 生成并保存报告
                            new Thread(() -> {
                                try {
                                    com.example.xinqiao.bean.TestRecord r = testRecordDao.getTestRecordById(reportId);
                                    TestReportEntity entity = new TestReportEntity();
                                    entity.userName = userName != null ? userName : "";
                                    entity.reportId = reportId;
                                    entity.type = (r != null && r.title != null) ? r.title : "心理测评";
                                    // 近似：按答题数量估算分数（每题20）
                                    int answered = (r != null) ? r.currentIndex : 0;
                                    entity.score = answered * 20;
                                    entity.riskLevel = (entity.score >= 80) ? "高风险" : (entity.score >= 60 ? "中风险" : "低风险");
                                    entity.date = (r != null && r.date != null) ? r.date : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                                    String detailsPlain = "报告ID: " + reportId +
                                            "\n测评名称: " + entity.type +
                                            "\n分数: " + entity.score +
                                            "\n风险等级: " + entity.riskLevel +
                                            "\n摘要: " + ((r != null && r.desc != null) ? r.desc : "已完成支付，报告生成成功") +
                                            "\n生成时间: " + entity.date;
                                    repo.addTestReport(entity, detailsPlain);
                                } catch (Exception ignore) {}
                                // 保存完成后重新加载显示内容
                                runOnUiThread(() -> loadReportContent());
                            }).start();
                        } else {
                            Toast.makeText(TestReportActivity.this, "状态更新失败，请联系客服", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(TestReportActivity.this, "支付失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
