package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审计日志数据访问接口
 *
 * @author CourseHub Team
 */
@Mapper
public interface AuditLogMapper {

    int insert(AuditLog log);

    List<AuditLog> selectPage(@Param("userId") Long userId,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);

    long countPage(@Param("userId") Long userId);
}
