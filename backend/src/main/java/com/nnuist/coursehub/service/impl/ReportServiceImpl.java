package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.dto.ReportCreateDTO;
import com.nnuist.coursehub.dto.ReportHandleDTO;
import com.nnuist.coursehub.entity.Report;
import com.nnuist.coursehub.mapper.ReportMapper;
import com.nnuist.coursehub.service.ReportService;
import com.nnuist.coursehub.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 举报业务实现
 *
 * <p>举报提交后进入 {@code PENDING} 状态，由管理员在后台处理。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    /** 允许的举报对象类型 */
    private static final List<String> VALID_TARGET_TYPES =
            Arrays.asList("RESOURCE", "COMMENT", "USER");

    /** 允许的处理结果 */
    private static final List<String> VALID_HANDLE_STATUS =
            Arrays.asList("HANDLED", "REJECTED");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(ReportCreateDTO dto) {
        Long uid = UserContext.getUserId();

        if (!VALID_TARGET_TYPES.contains(dto.getTargetType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "举报对象类型不合法");
        }
        // 防重复举报：同一个人对同一个对象的待处理举报只允许有一条
        if (reportMapper.countPending(uid, dto.getTargetType(), dto.getTargetId()) > 0) {
            throw new BizException(ResultCode.DUPLICATE_SUBMIT, "你已经举报过了，请等待处理");
        }

        Report report = new Report();
        report.setReporterId(uid);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDescription(dto.getDescription() == null ? "" : dto.getDescription());

        reportMapper.insert(report);

        log.info("提交举报 | reportId={} | target={}#{} | reason={}",
                report.getId(), dto.getTargetType(), dto.getTargetId(), dto.getReason());
        return report.getId();
    }

    @Override
    public PageResult<ReportVO> list(String status, Integer page, Integer pageSize) {
        int p = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);

        long total = reportMapper.countPage(status);
        if (total == 0) {
            return PageResult.empty(p, size);
        }
        List<ReportVO> list = reportMapper.selectPage(status, (p - 1) * size, size);
        return new PageResult<>(list, total, p, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long reportId, ReportHandleDTO dto) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        if (!VALID_HANDLE_STATUS.contains(dto.getStatus())) {
            throw new BizException(ResultCode.PARAM_ERROR, "处理结果不合法");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new BizException(ResultCode.OPERATION_NOT_ALLOWED, "该举报已经处理过了");
        }

        Report update = new Report();
        update.setId(reportId);
        update.setStatus(dto.getStatus());
        update.setHandlerId(UserContext.getUserId());
        update.setHandleNote(dto.getHandleNote() == null ? "" : dto.getHandleNote());
        reportMapper.updateHandleStatus(update);

        log.info("处理举报 | reportId={} | status={} | handlerId={}",
                reportId, dto.getStatus(), UserContext.getUserId());
    }
}
