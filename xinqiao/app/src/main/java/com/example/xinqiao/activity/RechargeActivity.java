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
import com.example.xinqiao.utils.AnalysisUtils;
import com.example.xinqiao.utils.PaymentUtils;

public class RechargeActivity extends AppCompatActivity {
    private TextView tvBalance;
    private EditText etAmount;
    private Button btnRecharge;
    private PaymentUtils paymentUtils;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recharge);
        
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

    private void initData() {
        paymentUtils = new PaymentUtils(this);
        updateBalance();
    }

    private void updateBalance() {
        paymentUtils.getBalance(userName, new PaymentUtils.PaymentCallback() {
            @Override
            public void onSuccess() {
                tvBalance.setText(String.format("当前余额：%.2f元", paymentUtils.getCurrentBalance()));
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RechargeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}