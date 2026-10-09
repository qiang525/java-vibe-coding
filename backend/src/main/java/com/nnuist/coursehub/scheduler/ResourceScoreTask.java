package com.nnuist.coursehub.scheduler;

import com.nnuist.coursehub.entity.Resource;
import com.nnuist.coursehub.entity.User;
import com.nnuist.coursehub.mapper.ResourceMapper;
import com.nnuist.coursehub.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资料可信度评分定时重算
 *
 * <p><b>为什么要定时算而不是实时算</b>：评分依赖下载量、收藏数等会变化的字段。
 * 如果每次用户访问列表都算一遍，10 条资料就是 10 次计算 + 多次查用户信用分，
 * 性能不可接受。<b>评分本来就是"允许有几分钟延迟"的数据</b>，
 * 定时批量算好存进字段、列表页直接读，是最划算的做法。</p>
 *
 * <p>这就是典型的<b>离线计算 + 在线读取</b>模式。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceScoreTask {

    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;
    private final ScoreCalculator scoreCalculator;

    /**
     * 全量重算（每天凌晨 3 点执行）
     *
     * <p>cron 表达式：秒 分 时 日 月 周</p>
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void recalculateDaily() {
        log.info("【定时任务】开始全量重算资料可信度评分");
        int count = recalculateAll();
        log.info("【定时任务】评分重算完成，共处理 {} 条资料", count);
    }

    /**
     * 全量重算的具体逻辑（也可以被手动触发）
     *
     * @return 处理的资料条数
     */
    public int recalculateAll() {
        List<Resource> list = resourceMapper.selectAllForScore();
        if (list.isEmpty()) {
            return 0;
        }

        // 先把用到的上传者信用分一次性查出来，避免循环里逐条查用户（N+1）
        Map<Long, Integer> creditMap = new HashMap<>();
        for (Resource r : list) {
            if (r.getUploaderId() != null && !creditMap.containsKey(r.getUploaderId())) {
                User u = userMapper.selectById(r.getUploaderId());
                creditMap.put(r.getUploaderId(), u == null ? 60 : (u.getCreditScore() == null ? 60 : u.getCreditScore()));
            }
        }

        int count = 0;
        for (Resource r : list) {
            BigDecimal score = scoreCalculator.calculate(r, creditMap.get(r.getUploaderId()));
            resourceMapper.updateScore(r.getId(), score);
            count++;
        }
        return count;
    }
}
