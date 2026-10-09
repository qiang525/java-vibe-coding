package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.entity.Comment;
import com.nnuist.coursehub.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论数据访问接口
 *
 * @author CourseHub Team
 */
@Mapper
public interface CommentMapper {

    /** 查资料下的一级评论（分页） */
    List<CommentVO> selectTopLevel(@Param("resourceId") Long resourceId,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);

    /** 查多条一级评论下的所有回复 */
    List<CommentVO> selectRepliesByRootIds(@Param("rootIds") List<Long> rootIds);

    /** 统计一级评论数 */
    long countTopLevel(@Param("resourceId") Long resourceId);

    Comment selectById(@Param("id") Long id);

    int insert(Comment comment);

    int softDelete(@Param("id") Long id);

    /** 回复数 +1 */
    int increaseReplyCount(@Param("id") Long id);
}
