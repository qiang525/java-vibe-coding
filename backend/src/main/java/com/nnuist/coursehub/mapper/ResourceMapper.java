package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.dto.ResourceQueryDTO;
import com.nnuist.coursehub.entity.Resource;
import com.nnuist.coursehub.vo.ResourceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资料数据访问接口
 *
 * <p><b>本接口的特点</b>：查询方法里都带了一个
 * {@code currentUserId} 参数，用于在 SQL 层就完成三级可见性过滤。
 * 这样既高效（不查出来再丢掉），又安全（漏写业务判断也不会泄露数据）。</p>
 *
 * @author CourseHub Team
 */
@Mapper
public interface ResourceMapper {

    /**
     * 分页查询资料列表（已按可见性过滤）
     */
    List<ResourceVO> selectPage(@Param("q") ResourceQueryDTO query);

    /**
     * 统计总数（条件必须与 selectPage 完全一致）
     */
    long countPage(@Param("q") ResourceQueryDTO query);

    /**
     * 查资料详情（<b>不做</b>可见性过滤）
     *
     * <p>刻意不做过滤，是为了在 Service 层区分两种结果：</p>
     * <ul>
     *   <li>查不到 → 404 资料不存在</li>
     *   <li>查到了但无权看 → 403 无权访问</li>
     * </ul>
     */
    ResourceVO selectVOById(@Param("id") Long id);

    /**
     * 查资料实体（Service 内部判断用）
     */
    Resource selectById(@Param("id") Long id);

    int insert(Resource resource);

    int update(Resource resource);

    /** 逻辑删除 */
    int softDelete(@Param("id") Long id);

    /** 浏览数 +1 */
    int increaseViewCount(@Param("id") Long id);

    /** 下载数 +1 */
    int increaseDownloadCount(@Param("id") Long id);
}
