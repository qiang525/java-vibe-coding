package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.entity.Report;
import com.nnuist.coursehub.vo.ReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 举报数据访问接口
 *
 * @author CourseHub Team
 */
@Mapper
public interface ReportMapper {

    List<ReportVO> selectPage(@Param("status") String status,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);

    long countPage(@Param("status") String status);

    Report selectById(@Param("id") Long id);

    int insert(Report report);

    int updateHandleStatus(Report report);

    /** 同一人对同一对象的待处理举报是否已存在（防重复举报） */
    int countPending(@Param("reporterId") Long reporterId,
                     @Param("targetType") String targetType,
                     @Param("targetId") Long targetId);
}
