package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.dto.CommentCreateDTO;
import com.nnuist.coursehub.entity.Comment;
import com.nnuist.coursehub.entity.Resource;
import com.nnuist.coursehub.mapper.CommentMapper;
import com.nnuist.coursehub.mapper.ResourceMapper;
import com.nnuist.coursehub.service.CommentService;
import com.nnuist.coursehub.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论业务实现
 *
 * <p><b>两级评论怎么查最高效</b>：<b>不</b>遍历一级评论逐条查回复（那是 N+1），
 * 而是先把一级评论查出来，收集它们的 ID，用一条 {@code WHERE root_id IN (...)}
 * 把全部回复捞回来，再在内存里分组贴回去。总共 2 次查询。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ResourceMapper resourceMapper;

    private static final String TYPE_NORMAL = "NORMAL";

    @Override
    public PageResult<CommentVO> listByResource(Long resourceId, Integer page, Integer pageSize) {
        int p = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);

        long total = commentMapper.countTopLevel(resourceId);
        if (total == 0) {
            return PageResult.empty(p, size);
        }

        // ① 查一级评论
        List<CommentVO> topList = commentMapper.selectTopLevel(resourceId, (p - 1) * size, size);
        if (topList.isEmpty()) {
            return PageResult.empty(p, size);
        }

        // ② 一次把所有这些一级评论的回复全查回来
        List<Long> rootIds = topList.stream().map(CommentVO::getId).collect(Collectors.toList());
        List<CommentVO> allReplies = commentMapper.selectRepliesByRootIds(rootIds);

        // ③ 在内存里按 rootId 分组
        Map<Long, List<CommentVO>> replyMap = allReplies.stream()
                .collect(Collectors.groupingBy(CommentVO::getRootId));

        Long uid = UserContext.getUserId();
        for (CommentVO vo : topList) {
            vo.setReplies(replyMap.getOrDefault(vo.getId(), Collections.emptyList()));
            fillCanDelete(vo, uid);
        }
        allReplies.forEach(r -> fillCanDelete(r, uid));

        return new PageResult<>(topList, total, p, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long resourceId, CommentCreateDTO dto) {
        Long uid = UserContext.getUserId();

        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }

        Comment comment = new Comment();
        comment.setResourceId(resourceId);
        comment.setUserId(uid);
        comment.setContent(dto.getContent());
        comment.setType(isBlank(dto.getType()) ? TYPE_NORMAL : dto.getType());

        if (dto.getParentId() != null) {
            // 这是一条回复
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BizException(ResultCode.COMMENT_NOT_FOUND);
            }
            if (!resourceId.equals(parent.getResourceId())) {
                throw new BizException(ResultCode.PARAM_ERROR, "父评论不属于该资料");
            }
            comment.setParentId(parent.getId());
            // rootId 指向一级评论：如果父评论本身就是一级评论，那它就是 root
            comment.setRootId(parent.getParentId() == null ? parent.getId() : parent.getRootId());

            // 父评论的回复数 +1
            commentMapper.increaseReplyCount(parent.getId());
        }
        // 一级评论：parentId / rootId 都留空

        commentMapper.insert(comment);

        // 资料的评论数 +1
        resourceMapper.increaseCommentCount(resourceId);

        log.info("发表评论 | commentId={} | resourceId={} | userId={}",
                comment.getId(), resourceId, uid);
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }

        Long uid = UserContext.getUserId();
        // 只有本人或管理员能删
        if (!uid.equals(comment.getUserId()) && !UserContext.isAdmin()) {
            log.warn("越权删除评论被拒绝 | commentId={} | ownerId={} | operatorId={}",
                    commentId, comment.getUserId(), uid);
            throw new BizException(ResultCode.FORBIDDEN);
        }

        commentMapper.softDelete(commentId);
        resourceMapper.decreaseCommentCount(comment.getResourceId());

        log.info("删除评论 | commentId={} | operatorId={}", commentId, uid);
    }

    private void fillCanDelete(CommentVO vo, Long uid) {
        if (uid == null) {
            vo.setCanDelete(false);
            return;
        }
        vo.setCanDelete(uid.equals(vo.getUserId()) || UserContext.isAdmin());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
