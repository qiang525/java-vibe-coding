package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体 —— 对应数据库表 comment
 *
 * <p><b>两级结构</b>：一级评论（{@code parentId = null}）直接挂在资料下，
 * 二级回复挂在某条一级评论下。</p>
 *
 * <pre>
 *   一级评论 A
 *     ├── 回复 A-1
 *     └── 回复 A-2
 *   一级评论 B
 * </pre>
 *
 * <p>{@code rootId} 记录"我属于哪条一级评论"，这样查某条评论下的所有回复
 * 只需一次 {@code WHERE root_id = ?}，不用递归。</p>
 *
 * @author CourseHub Team
 */
@Data
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long resourceId;
    private Long userId;

    /** 父评论 ID（null = 一级评论） */
    private Long parentId;

    /** 根评论 ID（楼中楼聚合用） */
    private Long rootId;

    /** 类型：NORMAL 讨论 / ERROR 纠错 / SUPPLEMENT 补充 / REPORT 举报可疑 */
    private String type;

    private String content;

    private Integer likeCount;
    private Integer replyCount;
    private Integer isTop;

    /** NORMAL / HIDDEN（被管理员隐藏） */
    private String status;

    private LocalDateTime createdAt;

    private Integer isDeleted;
}
