package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.vo.CollectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收藏数据访问接口
 *
 * @author CourseHub Team
 */
@Mapper
public interface CollectionMapper {

    /** 我的收藏列表（JOIN 出资料信息） */
    List<CollectionVO> selectPage(@Param("userId") Long userId,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    long countPage(@Param("userId") Long userId);

    /** 是否已收藏 */
    int countByUserAndResource(@Param("userId") Long userId,
                               @Param("resourceId") Long resourceId);

    int insert(@Param("userId") Long userId,
               @Param("resourceId") Long resourceId,
               @Param("folder") String folder);

    int delete(@Param("userId") Long userId,
               @Param("resourceId") Long resourceId);
}
