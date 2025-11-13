package com.example.xinqiao.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
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
    // 新增：待支付页信息与交互控件
    private Button btnRecharge;
    private TextView tvTitle;
    private TextView tvStatus;
    private TextView tvPrice;
    private TextView tvReportId;
    private TextView tvDate;
    private TextView tvBalance;
    private LinearLayout payContainer;
    private static final double REPORT_PRICE = 9.9; // 报告价格
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_report);
        
        // 初始化组件
        tvReportContent = findViewById(R.id.tv_report_content);
        btnPay = findViewById(R.id.btn_pay);
        btnRecharge = findViewById(R.id.btn_recharge);
        tvTitle = findViewById(R.id.tv_title);
        tvStatus = findViewById(R.id.tv_status);
        tvPrice = findViewById(R.id.tv_price);
        tvReportId = findViewById(R.id.tv_report_id);
        tvDate = findViewById(R.id.tv_date);
        tvBalance = findViewById(R.id.tv_balance);
        payContainer = findViewById(R.id.pay_container);
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        // 获取数据
        reportId = getIntent().getStringExtra("reportId");
        userName = AnalysisUtils.readLoginUserName(this);
        testRecordDao = new TestRecordDao(this);
        paymentUtils = new PaymentUtils(this);
        repo = new MedicalRecordRepository(this);
        
        // 填充顶部信息与余额
        populateHeader();
        refreshBalance();

        // 加载报告内容
        loadReportContent();
        
        // 检查报告状态并设置UI
        checkReportStatus();
        
        // 设置支付按钮点击事件
        btnPay.setOnClickListener(v -> handlePayment());
        if (btnRecharge != null) {
            btnRecharge.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(TestReportActivity.this, RechargeActivity.class);
                startActivity(intent);
            });
        }
    }

    // 填充顶部信息：标题、状态、价格、报告ID与日期
    private void populateHeader() {
        if (tvPrice != null) {
            tvPrice.setText(String.format(java.util.Locale.getDefault(), "¥%.1f", REPORT_PRICE));
        }
        if (tvReportId != null) {
            tvReportId.setText("报告ID: " + (reportId != null ? reportId : "--"));
        }
        new Thread(() -> {
            com.example.xinqiao.bean.TestRecord r = null;
            try { r = testRecordDao.getTestRecordById(reportId); } catch (Exception ignore) {}
            final com.example.xinqiao.bean.TestRecord fr = r;
            runOnUiThread(() -> {
                if (fr != null) {
                    if (tvTitle != null && fr.title != null) tvTitle.setText(fr.title);
                    if (tvDate != null) tvDate.setText("日期: " + (fr.date != null ? fr.date : "--"));
                    if (tvStatus != null) {
                        int st = fr.status;
                        tvStatus.setText(st == 1 ? "已完成" : (st == 2 ? "待支付" : "未完成"));
                    }
                } else {
                    if (tvTitle != null) tvTitle.setText("心理测评");
                    if (tvDate != null) tvDate.setText("日期: --");
                    if (tvStatus != null) tvStatus.setText("待支付");
                }
            });
        }).start();
    }

    // 刷新余额显示
    private void refreshBalance() {
        paymentUtils.getBalance(userName, new PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (tvBalance != null) {
                        tvBalance.setText(String.format(java.util.Locale.getDefault(), "余额：¥%.2f", paymentUtils.getCurrentBalance()));
                    }
                });
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (tvBalance != null) tvBalance.setText("余额：获取失败");
                });
            }
        });
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
                        if (details == null || details.isEmpty()) {
                            // 详情缺失或解密失败：不显示占位，尝试重建并回填
                            tvReportContent.setText("");
                            attemptAutoGenerateFromRecordAndUpdateEntity();
                        } else {
                            tvReportContent.setText(details);
                        }
                    } else {
                        // 兜底：若按用户名查询不到，尝试仅按 reportId 查询
                        repo.getTestReportByReportIdAnyUserAsync(reportId, new MedicalRecordRepository.TestReportEntityCallback() {
                            @Override
                            public void onSuccess(TestReportEntity anyUserEntity) {
                                runOnUiThread(() -> {
                                    if (anyUserEntity != null) {
                                        String details2 = anyUserEntity.detailsEncrypted != null ? CryptoUtil.decrypt(anyUserEntity.detailsEncrypted) : "";
                                        if (details2 == null || details2.isEmpty()) {
                                            tvReportContent.setText("");
                                            attemptAutoGenerateFromRecordAndUpdateEntity();
                                        } else {
                                            tvReportContent.setText(details2);
                                        }
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
                    runOnUiThread(() -> tvReportContent.setText(""));
                }
            } else if (status == 2) {
                runOnUiThread(() -> tvReportContent.setText(""));
            } else {
                runOnUiThread(() -> tvReportContent.setText(""));
            }
        }).start();
    }

    /**
     * 当存在报告实体但详情缺失或解密失败时，尝试根据TestRecord重建明文并持久化更新detailsEncrypted。
     */
    private void attemptAutoGenerateFromRecordAndUpdateEntity() {
        new Thread(() -> {
            try {
                com.example.xinqiao.bean.TestRecord r = testRecordDao.getTestRecordById(reportId);
                if (r == null) return;
                String type = (r.title != null) ? r.title : "心理测评";
                int answered = r.currentIndex;
                int score = answered * 20;
                String riskLevel = (score >= 80) ? "高风险" : (score >= 60 ? "中风险" : "低风险");
                String date = (r.date != null) ? r.date : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                String detailsPlain = "报告ID: " + reportId +
                        "\n测评名称: " + type +
                        "\n分数: " + score +
                        "\n风险等级: " + riskLevel +
                        "\n摘要: " + ((r.desc != null) ? r.desc : "已完成测评，自动生成报告") +
                        "\n生成时间: " + date;
                // 更新持久化内容（内部含加密/PLA回退逻辑）
                repo.updateTestReportDetails(reportId, detailsPlain);
                // 刷新展示
                runOnUiThread(this::loadReportContent);
            } catch (Exception ignore) {}
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
                    btnPay.setVisibility(View.GONE);
                    if (payContainer != null) payContainer.setVisibility(View.GONE);
                    if (tvStatus != null) tvStatus.setText("已完成");
                } else if (status == 2) { // 待支付
                    btnPay.setVisibility(View.VISIBLE);
                    btnPay.setText("支付 ¥" + REPORT_PRICE);
                    if (payContainer != null) payContainer.setVisibility(View.VISIBLE);
                    if (tvStatus != null) tvStatus.setText("待支付");
                } else { // 其他状态
                    btnPay.setVisibility(View.GONE);
                    if (payContainer != null) payContainer.setVisibility(View.GONE);
                    if (tvStatus != null) tvStatus.setText("未完成");
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
                            if (payContainer != null) payContainer.setVisibility(View.GONE);
                            if (tvStatus != null) tvStatus.setText("已完成");
                            refreshBalance();
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
