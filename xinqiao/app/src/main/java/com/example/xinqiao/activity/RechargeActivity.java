package com.example.xinqiao.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xinqiao.R;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.util.payment.PaymentUtils;
import com.google.android.material.button.MaterialButton;

public class RechargeActivity extends AppCompatActivity {
    private TextView tvBalance;
    private EditText etAmount;
    private Button btnRecharge;
    private MaterialButton btnAmount50, btnAmount100, btnAmount200, btnAmount500;
    private PaymentUtils paymentUtils;
    private String userName;
    private double selectedAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge_new);
        
        userName = AnalysisUtils.readLoginUserName(this);
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        initData();
    }

    private void initView() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        tvBalance = findViewById(R.id.tv_balance);
        etAmount = findViewById(R.id.et_amount);
        btnRecharge = findViewById(R.id.btn_recharge);
        
        // 快速金额按钮
        btnAmount50 = findViewById(R.id.btn_amount_50);
        btnAmount100 = findViewById(R.id.btn_amount_100);
        btnAmount200 = findViewById(R.id.btn_amount_200);
        btnAmount500 = findViewById(R.id.btn_amount_500);

        // 快速金额按钮点击事件
        View.OnClickListener amountClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 重置所有按钮状态
                resetAmountButtons();
                
                // 设置选中状态
                MaterialButton clickedButton = (MaterialButton) v;
                clickedButton.setStrokeColorResource(android.R.color.white);
                clickedButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                clickedButton.setTextColor(getResources().getColor(android.R.color.white));
                
                // 设置金额
                if (v == btnAmount50) {
                    selectedAmount = 50;
                    etAmount.setText("50");
                } else if (v == btnAmount100) {
                    selectedAmount = 100;
                    etAmount.setText("100");
                } else if (v == btnAmount200) {
                    selectedAmount = 200;
                    etAmount.setText("200");
                } else if (v == btnAmount500) {
                    selectedAmount = 500;
                    etAmount.setText("500");
                }
            }
        };
        
        btnAmount50.setOnClickListener(amountClickListener);
        btnAmount100.setOnClickListener(amountClickListener);
        btnAmount200.setOnClickListener(amountClickListener);
        btnAmount500.setOnClickListener(amountClickListener);
        
        // 输入框监听，清除快速选择
        etAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    resetAmountButtons();
                }
            }
        });

        btnRecharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = etAmount.getText().toString().trim();
                if (TextUtils.isEmpty(amountStr)) {
                    Toast.makeText(RechargeActivity.this, "请输入充值金额", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        Toast.makeText(RechargeActivity.this, "请输入正确的充值金额", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 执行充值
                    paymentUtils.recharge(userName, amount, new PaymentUtils.PaymentCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(RechargeActivity.this, "充值成功", Toast.LENGTH_SHORT).show();
                            updateBalance();
                            etAmount.setText("");
                            resetAmountButtons();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(RechargeActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(RechargeActivity.this, "请输入正确的充值金额", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void resetAmountButtons() {
        // 重置所有按钮为未选中状态
        btnAmount50.setStrokeColorResource(R.color.colorPrimary);
        btnAmount50.setBackgroundColor(getResources().getColor(android.R.color.white));
        btnAmount50.setTextColor(getResources().getColor(R.color.colorPrimary));
        
        btnAmount100.setStrokeColorResource(R.color.colorPrimary);
        btnAmount100.setBackgroundColor(getResources().getColor(android.R.color.white));
        btnAmount100.setTextColor(getResources().getColor(R.color.colorPrimary));
        
        btnAmount200.setStrokeColorResource(R.color.colorPrimary);
        btnAmount200.setBackgroundColor(getResources().getColor(android.R.color.white));
        btnAmount200.setTextColor(getResources().getColor(R.color.colorPrimary));
        
        btnAmount500.setStrokeColorResource(R.color.colorPrimary);
        btnAmount500.setBackgroundColor(getResources().getColor(android.R.color.white));
        btnAmount500.setTextColor(getResources().getColor(R.color.colorPrimary));
    }

    private void initData() {
        paymentUtils = new PaymentUtils(this);
        updateBalance();
    }

    private void updateBalance() {
        paymentUtils.getBalance(userName, new PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                tvBalance.setText(String.format("%.2f", paymentUtils.getCurrentBalance()));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RechargeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
