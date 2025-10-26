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

public class TestReportActivity extends AppCompatActivity {
    private String reportId;
    private TextView tvReportContent;
    private Button btnPay;
    private TestRecordDao testRecordDao;
    private PaymentUtils paymentUtils;
    private String userName;
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
        // 这里可以根据reportId从数据库加载完整报告内容
        // 目前使用简单文本展示
        tvReportContent.setText("报告ID: " + reportId + "\n这里展示测评报告详情内容...\n\n完整报告需要支付才能查看。");
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
                    tvReportContent.setText("报告ID: " + reportId + "\n\n完整报告内容...\n\n感谢您的支持！");
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
                            tvReportContent.setText("报告ID: " + reportId + "\n\n完整报告内容...\n\n感谢您的支持！");
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