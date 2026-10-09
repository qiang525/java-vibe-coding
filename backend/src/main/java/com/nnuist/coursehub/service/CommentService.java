package com.nnuist.coursehub.service;

import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.dto.CommentCreateDTO;
import com.nnuist.coursehub.vo.CommentVO;

/**
 * 评论业务接口
 *
 * @author CourseHub Team
 */
public interface CommentService {

    /**
     * 查资料下的评论（两级结构：一级评论里嵌回复）
     */
    PageResult<CommentVO> listByResource(Long resourceId, Integer page, Integer pageSize);

    /**
     * 发表评论或回复
     *
     * @return 新评论 ID
     */
    Long create(Long resourceId, CommentCreateDTO dto);

    /**
     * 删除评论（本人或管理员）
     */
    void delete(Long commentId);
}
