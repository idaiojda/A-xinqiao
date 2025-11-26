package com.example.xinqiao.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive bar chart component for consultation statistics
 * Supports week/month view switching with smooth animations
 */
public class ConsultationBarChart extends View {
    
    private static final int ANIMATION_DURATION = 800;
    private static final int BAR_CORNER_RADIUS = 8;
    private static final int TEXT_SIZE = 12;
    private static final int AXIS_TEXT_SIZE = 10;
    
    private Paint barPaint;
    private Paint axisPaint;
    private Paint textPaint;
    private Paint gridPaint;
    
    private List<Float> dataValues;
    private List<String> labels;
    private List<Integer> barColors;
    
    private float maxValue = 0;
    private float animationProgress = 0f;
    private boolean isWeekView = true;
    
    private int viewWidth;
    private int viewHeight;
    private int barWidth;
    private int barSpacing;
    private int leftMargin = 60;
    private int rightMargin = 20;
    private int topMargin = 20;
    private int bottomMargin = 40;
    
    public ConsultationBarChart(Context context) {
        super(context);
        init();
    }
    
    public ConsultationBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ConsultationBarChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        axisPaint.setColor(Color.parseColor("#6B7280"));
        axisPaint.setStrokeWidth(2f);
        axisPaint.setTextSize(AXIS_TEXT_SIZE * getResources().getDisplayMetrics().density);
        
        textPaint.setColor(Color.parseColor("#374151"));
        textPaint.setTextSize(TEXT_SIZE * getResources().getDisplayMetrics().density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        gridPaint.setColor(Color.parseColor("#E5E7EB"));
        gridPaint.setStrokeWidth(1f);
        
        dataValues = new ArrayList<>();
        labels = new ArrayList<>();
        barColors = new ArrayList<>();
        
        // Default week data
        setWeekData();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        if (dataValues.size() > 0) {
            int availableWidth = viewWidth - leftMargin - rightMargin;
            barSpacing = availableWidth / (dataValues.size() * 2);
            barWidth = barSpacing;
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (dataValues.isEmpty()) return;
        
        drawGrid(canvas);
        drawBars(canvas);
        drawAxes(canvas);
        drawLabels(canvas);
    }
    
    private void drawGrid(Canvas canvas) {
        int chartHeight = viewHeight - topMargin - bottomMargin;
        int gridLines = 5;
        
        for (int i = 0; i <= gridLines; i++) {
            float y = topMargin + (chartHeight * i / gridLines);
            canvas.drawLine(leftMargin, y, viewWidth - rightMargin, y, gridPaint);
        }
    }
    
    private void drawBars(Canvas canvas) {
        int chartHeight = viewHeight - topMargin - bottomMargin;
        int startX = leftMargin + barSpacing;
        
        for (int i = 0; i < dataValues.size(); i++) {
            float value = dataValues.get(i);
            float barHeight = (value / maxValue) * chartHeight * animationProgress;
            
            float left = startX + (i * 2 * barSpacing);
            float top = viewHeight - bottomMargin - barHeight;
            float right = left + barWidth;
            float bottom = viewHeight - bottomMargin;
            
            RectF barRect = new RectF(left, top, right, bottom);
            
            // Apply gradient effect
            int color = barColors.get(i);
            barPaint.setColor(color);
            
            // Draw rounded bar
            canvas.drawRoundRect(barRect, BAR_CORNER_RADIUS, BAR_CORNER_RADIUS, barPaint);
            
            // Draw value on top of bar
            if (animationProgress > 0.8f) {
                String valueText = String.valueOf(Math.round(value));
                float textY = top - 8;
                textPaint.setColor(Color.WHITE);
                canvas.drawText(valueText, left + barWidth / 2, textY, textPaint);
            }
        }
    }
    
    private void drawAxes(Canvas canvas) {
        // Y-axis
        canvas.drawLine(leftMargin, topMargin, leftMargin, viewHeight - bottomMargin, axisPaint);
        
        // X-axis
        canvas.drawLine(leftMargin, viewHeight - bottomMargin, viewWidth - rightMargin, viewHeight - bottomMargin, axisPaint);
    }
    
    private void drawLabels(Canvas canvas) {
        int startX = leftMargin + barSpacing;
        
        for (int i = 0; i < labels.size(); i++) {
            float x = startX + (i * 2 * barSpacing) + barWidth / 2;
            float y = viewHeight - bottomMargin + 20;
            
            textPaint.setColor(Color.parseColor("#6B7280"));
            canvas.drawText(labels.get(i), x, y, textPaint);
        }
        
        // Draw Y-axis labels
        int chartHeight = viewHeight - topMargin - bottomMargin;
        int gridLines = 5;
        
        for (int i = 0; i <= gridLines; i++) {
            float value = maxValue * (gridLines - i) / gridLines;
            float y = topMargin + (chartHeight * i / gridLines) + 5;
            
            textPaint.setColor(Color.parseColor("#6B7280"));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(Math.round(value)), leftMargin - 10, y, textPaint);
        }
        
        textPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    public void setWeekData() {
        isWeekView = true;
        dataValues.clear();
        labels.clear();
        barColors.clear();
        
        // Week data (Monday to Sunday)
        dataValues.add(8f);
        dataValues.add(12f);
        dataValues.add(15f);
        dataValues.add(10f);
        dataValues.add(18f);
        dataValues.add(22f);
        dataValues.add(16f);
        
        labels.add("一");
        labels.add("二");
        labels.add("三");
        labels.add("四");
        labels.add("五");
        labels.add("六");
        labels.add("日");
        
        // Professional blue-green gradient
        barColors.add(Color.parseColor("#667eea"));
        barColors.add(Color.parseColor("#5a67d8"));
        barColors.add(Color.parseColor("#4c51bf"));
        barColors.add(Color.parseColor("#38b2ac"));
        barColors.add(Color.parseColor("#319795"));
        barColors.add(Color.parseColor("#2c7a7b"));
        barColors.add(Color.parseColor("#285e61"));
        
        maxValue = 25f;
        animateChart();
    }
    
    public void setMonthData() {
        isWeekView = false;
        dataValues.clear();
        labels.clear();
        barColors.clear();
        
        // Month data (last 4 weeks)
        dataValues.add(65f);
        dataValues.add(78f);
        dataValues.add(92f);
        dataValues.add(85f);
        
        labels.add("第1周");
        labels.add("第2周");
        labels.add("第3周");
        labels.add("第4周");
        
        // Professional blue-green gradient
        barColors.add(Color.parseColor("#667eea"));
        barColors.add(Color.parseColor("#5a67d8"));
        barColors.add(Color.parseColor("#4c51bf"));
        barColors.add(Color.parseColor("#38b2ac"));
        
        maxValue = 100f;
        animateChart();
    }
    
    private void animateChart() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animationProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        
        animator.start();
    }
    
    public boolean isWeekView() {
        return isWeekView;
    }
    
    public void toggleView() {
        if (isWeekView) {
            setMonthData();
        } else {
            setWeekData();
        }
    }
}