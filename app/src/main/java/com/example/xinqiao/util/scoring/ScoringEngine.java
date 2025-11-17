package com.example.xinqiao.util.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoringEngine {

    public static class Factors {
        public double behaviorActivity;
        public double behaviorCompletion;
        public double behaviorConsistency;
        public double qualityAccuracy;
        public double qualityTimeliness;
        public double objectiveVerification;
    }

    public static class Result {
        public double baseScore;
        public double weightedScore;
        public double adjustedScore;
        public String grade;
        public Map<String, Double> breakdown = new LinkedHashMap<>();
        public List<String> explanation = new ArrayList<>();
    }

    public static Result evaluate(Factors f, long lastUpdateMillis, long nowMillis) {
        Result r = new Result();

        double[] vals = new double[]{
                clamp01(f.behaviorActivity),
                clamp01(f.behaviorCompletion),
                clamp01(f.behaviorConsistency),
                clamp01(f.qualityAccuracy),
                clamp01(f.qualityTimeliness),
                clamp01(f.objectiveVerification)
        };
        double base = avg(vals) * 100.0;
        r.baseScore = round2(base);

        double wBehaviorActivity = 0.15;
        double wBehaviorCompletion = 0.25;
        double wBehaviorConsistency = 0.10;
        double wQualityAccuracy = 0.30;
        double wQualityTimeliness = 0.10;
        double wObjectiveVerification = 0.10;

        double weighted = 100.0 * (
                wBehaviorActivity * vals[0] +
                wBehaviorCompletion * vals[1] +
                wBehaviorConsistency * vals[2] +
                wQualityAccuracy * vals[3] +
                wQualityTimeliness * vals[4] +
                wObjectiveVerification * vals[5]
        );
        r.weightedScore = round2(weighted);

        long ageMillis = Math.max(0, nowMillis - lastUpdateMillis);
        double halfLifeDays = 30.0;
        double lambda = Math.log(2) / (halfLifeDays * 24 * 60 * 60 * 1000.0);
        double decay = Math.exp(-lambda * ageMillis);
        double adjusted = weighted * decay;
        r.adjustedScore = round2(adjusted);

        r.breakdown.put("行为-活跃度", round2(100.0 * vals[0] * wBehaviorActivity));
        r.breakdown.put("行为-完成度", round2(100.0 * vals[1] * wBehaviorCompletion));
        r.breakdown.put("行为-一致性", round2(100.0 * vals[2] * wBehaviorConsistency));
        r.breakdown.put("质量-准确性", round2(100.0 * vals[3] * wQualityAccuracy));
        r.breakdown.put("质量-及时性", round2(100.0 * vals[4] * wQualityTimeliness));
        r.breakdown.put("客观-第三方验证", round2(100.0 * vals[5] * wObjectiveVerification));

        r.explanation.add("基础评分=" + round2(avg(vals) * 100.0));
        r.explanation.add("加权评分=" + r.weightedScore);
        r.explanation.add("时间衰减系数=" + round4(decay));
        r.explanation.add("动态调整后评分=" + r.adjustedScore);

        r.grade = gradeFromScore(r.adjustedScore);
        return r;
    }

    private static String gradeFromScore(double s) {
        if (s >= 90) return "A+";
        if (s >= 80) return "A";
        if (s >= 65) return "B";
        if (s >= 50) return "C";
        return "D";
    }

    private static double avg(double[] a) {
        double sum = 0;
        for (double v : a) sum += v;
        return a.length == 0 ? 0 : sum / a.length;
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}

