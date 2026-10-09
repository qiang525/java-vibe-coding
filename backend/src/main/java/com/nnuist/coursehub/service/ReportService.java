package com.nnuist.coursehub.service;

import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.dto.ReportCreateDTO;
import com.nnuist.coursehub.dto.ReportHandleDTO;
import com.nnuist.coursehub.vo.ReportVO;

/**
 * 举报业务接口
 *
 * @author CourseHub Team
 */
public interface ReportService {

    /** 提交举报 */
    Long submit(ReportCreateDTO dto);

    /** 举报列表（管理员） */
    PageResult<ReportVO> list(String status, Integer page, Integer pageSize);

    /** 处理举报（管理员） */
    void handle(Long reportId, ReportHandleDTO dto);
}
