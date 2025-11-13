package com.example.xinqiao.bean;
import java.io.Serializable;
import java.util.List;

public class ExercisesBean implements Serializable {
	public int id;// 每章习题id
	public String title;// 每章习题标题
	public String content;// 每章习题的数目
	public int background;// 每章习题前边的序号背景
	public int subjectId;// 每道习题的Id
	public String subject;// 每道习题的题干
	public String a;// 每道题的A选项
	public String b;// 每道题的B选项
	public String c;// 每道题的C选项
	public String d;// 每道题的D选项
	public int answer;// 每道题的正确答案
	public int select;// 用户选中的那项（0表示所选项对了，1表示A选项错，2表示B选项错，3表示C选项错，4表示D选项错）
	public List<QuestionBean> questions; // 题目列表
	public String category; // 分类
	public double price = 0.0; // 习题价格，默认为0表示免费
	public boolean isPremium = false; // 是否为付费习题
	public boolean isPurchased = false; // 用户是否已购买

    public String getAnalysis(int score, int total) {
        float percent = total > 0 ? (float) score / total : 0f;
        switch (id) {
            case 1: // 恋爱心理成熟度
                if (percent > 0.8) return "你的恋爱心理非常成熟，能够理性处理感情问题。";
                else if (percent > 0.5) return "你的恋爱心理较为成熟，但仍有提升空间。";
                else return "你的恋爱心理还需成长，建议多学习沟通与自我调节。";
            case 2: // 社交恐惧
                if (percent > 0.8) return "你几乎没有社交恐惧，社交能力很强。";
                else if (percent > 0.5) return "你有轻微社交紧张，建议多参与集体活动。";
                else return "你存在较明显的社交恐惧，建议逐步锻炼自信。";
            case 3: // 交友能力
                if (percent > 0.8) return "你的交友能力很强，善于与人沟通。";
                else if (percent > 0.5) return "你的交友能力一般，可以多主动结识朋友。";
                else return "你在人际交往上较为被动，建议多锻炼社交技巧。";
            case 4: // 抑郁量表
                if (percent > 0.8) return "你几乎没有抑郁倾向，心理状态良好。";
                else if (percent > 0.5) return "你有轻微抑郁情绪，建议多关注自我调节。";
                else return "你存在较明显的抑郁倾向，建议及时寻求专业帮助。";
            case 5: // 焦虑
                if (percent > 0.8) return "你的焦虑水平很低，情绪管理良好。";
                else if (percent > 0.5) return "你有一定焦虑情绪，建议适当放松。";
                else return "你存在较高的焦虑水平，建议学习压力管理技巧。";
            case 6: // 抑郁症
                if (percent > 0.8) return "你没有明显抑郁症状，心理健康。";
                else if (percent > 0.5) return "你有轻度抑郁表现，建议多与人交流。";
                else return "你有较明显抑郁症状，建议寻求心理咨询。";
            case 7: // 精神压力
                if (percent > 0.8) return "你的压力管理能力很强。";
                else if (percent > 0.5) return "你有一定压力，建议适当放松。";
                else return "你压力较大，建议调整生活节奏。";
            case 8: // 抑郁应对
                if (percent > 0.8) return "你的应对方式积极有效。";
                else if (percent > 0.5) return "你的应对方式一般，可多尝试积极方法。";
                else return "你的应对方式较消极，建议学习情绪调节。";
            case 9: // 回避型依恋
                if (percent > 0.8) return "你很少有回避型依恋，亲密关系健康。";
                else if (percent > 0.5) return "你有一定回避型依恋倾向，建议多信任他人。";
                else return "你回避型依恋明显，建议多尝试开放自我。";
            case 10: // 人生质量
                if (percent > 0.8) return "你的人生质量很高，生活幸福感强。";
                else if (percent > 0.5) return "你的人生质量一般，可多关注自我成长。";
                else return "你对生活满意度较低，建议调整心态，积极面对生活。";
            default:
                return "暂无分析。";
        }
    }
}