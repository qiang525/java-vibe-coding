package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.entity.ResourceVersion;
import com.nnuist.coursehub.vo.VersionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资料版本数据访问接口
 *
 * <p>本阶段（M2）只用到"新建 v1 版本"和"查版本"两个能力。
 * 分支、合并、回滚在 M3 实现，届时在这里加方法即可。</p>
 *
 * @author CourseHub Team
 */
@Mapper
public interface ResourceVersionMapper {

    int insert(ResourceVersion version);

    ResourceVersion selectById(@Param("id") Long id);

    /**
     * 查某资料的所有版本（按版本号倒序）
     */
    List<ResourceVersion> selectByResourceId(@Param("resourceId") Long resourceId);

    /**
     * 查询某资料的版本列表（含文件名、编辑者昵称）
     */
    List<VersionVO> listVersions(@Param("resourceId") Long resourceId);

    /**
     * 统计某资料的最新版本号（用于生成下一个版本号）
     */
    Integer selectMaxVersionNo(@Param("resourceId") Long resourceId,
                               @Param("branch") String branch);
}
