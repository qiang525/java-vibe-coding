package com.nnuist.coursehub.scheduler;

import com.nnuist.coursehub.entity.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 可信度评分计算器
 *
 * <p><b>为什么需要"可信度评分"</b>：课程资料最容易出现的问题就是
 * "不知道哪份靠谱"。全部按时间倒序排，最新上传的可能质量很差；
 * 全部按下载量排，热门资料会一直霸榜。</p>
 *
 * <p>所以设计了一个<b>多因子加权</b>的评分模型：</p>
 *
 * <pre>
 * score = 0.25 × 资料完整度     （描述、章节、文件大小是否齐全）
 *       + 0.20 × 上传者信用      （上传者的 credit_score）
 *       + 0.20 × 社区评价        （用户打分，0-5 映射到 0-100）
 *       + 0.25 × 社区认可度      （下载 / 收藏 / 评论，取对数避免头部垄断）
 *       + 0.10 × 时效性          （每过一学期衰减 15%）
 * </pre>
 *
 * <p><b>为什么认可度要用对数</b>：直接拿下载量算，一个 10000 次下载的老资料
 * 会把新上传的优质资料彻底压死。取 {@code log10} 后，100 次和 10000 次的
 * 差距从 100 倍缩小到 2 倍，新资料有机会冒头。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Component
public class ScoreCalculator {

    @Value("${coursehub.score.weight.completeness:0.25}")
    private double wCompleteness;

    @Value("${coursehub.score.weight.uploader:0.20}")
    private double wUploader;

    @Value("${coursehub.score.weight.rating:0.20}")
    private double wRating;

    @Value("${coursehub.score.weight.recognition:0.25}")
    private double wRecognition;

    @Value("${coursehub.score.weight.freshness:0.10}")
    private double wFreshness;

    /** 每学期（约 6 个月）时效性衰减比例 */
    @Value("${coursehub.score.freshness-decay-per-term:0.15}")
    private double decayPerTerm;

    /** 认可度归一化基准：这个组合分算满分 */
    private static final double RECOGNITION_BASE = 1000.0;

    /**
     * 计算一条资料的可信度评分
     *
     * @param r               资料
     * @param uploaderCredit  上传者信用分（0-100）
     * @return 0-100 的评分，保留两位小数
     */
    public BigDecimal calculate(Resource r, Integer uploaderCredit) {
        double completeness = completeness(r);
        double uploader = uploaderCredit == null ? 60 : Math.min(100, Math.max(0, uploaderCredit));
        double rating = rating(r);
        double recognition = recognition(r);
        double freshness = freshness(r);

        double score = wCompleteness * completeness
                + wUploader * uploader
                + wRating * rating
                + wRecognition * recognition
                + wFreshness * freshness;

        // 夹在 0-100 之间，防止配置权重之和不为 1 时溢出
        score = Math.min(100.0, Math.max(0.0, score));

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /** 因子①：资料完整度 */
    private double completeness(Resource r) {
        double score = 0;
        if (r.getDescription() != null && r.getDescription().trim().length() >= 10) {
            score += 35;  // 有像样的描述
        }
        if (r.getChapterId() != null) {
            score += 25;  // 归到了具体章节
        }
        if (r.getTitle() != null && r.getTitle().length() >= 4) {
            score += 20;  // 标题不是"新建文档"这种敷衍名字
        }
        if (r.getDownloadCount() != null || r.getViewCount() != null) {
            score += 20;  // 有文件（能查到统计说明文件在）
        }
        return Math.min(100, score);
    }

    /** 因子②：社区评价（0-5 分映射到 0-100） */
    private double rating(Resource r) {
        if (r.getAvgRating() == null || r.getRatingCount() == null || r.getRatingCount() == 0) {
            // 没人评分时给个中性分，而不是 0 —— 否则新资料永远起不来
            return 60;
        }
        return r.getAvgRating().doubleValue() / 5.0 * 100.0;
    }

    /**
     * 因子③：社区认可度
     *
     * <p>下载 2 分、收藏 3 分、评论 1 分，加权求和后取对数归一化。</p>
     */
    private double recognition(Resource r) {
        double raw = safe(r.getDownloadCount()) * 2.0
                + safe(r.getFavoriteCount()) * 3.0
                + safe(r.getCommentCount()) * 1.0;
        if (raw <= 0) {
            return 0;
        }
        // log10(1+x) / log10(1+BASE) → 0-100
        double norm = Math.log10(1 + raw) / Math.log10(1 + RECOGNITION_BASE) * 100.0;
        return Math.min(100, norm);
    }

    /**
     * 因子④：时效性（指数衰减）
     *
     * <p>公式：{@code (1 - 0.15)^学期数 × 100}。
     * 上传满 1 学期约 85 分，2 学期约 72 分，3 学期约 61 分。</p>
     */
    private double freshness(Resource r) {
        if (r.getCreatedAt() == null) {
            return 100;
        }
        long months = ChronoUnit.MONTHS.between(r.getCreatedAt(), LocalDateTime.now());
        if (months < 0) {
            months = 0;
        }
        double terms = months / 6.0;   // 6 个月 = 1 学期
        double factor = Math.pow(1 - decayPerTerm, terms);
        return Math.max(0, Math.min(100, factor * 100));
    }

    private int safe(Integer v) {
        return v == null ? 0 : v;
    }
}
