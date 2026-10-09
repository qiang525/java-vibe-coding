package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志 —— 对应数据库表 audit_log
 *
 * <p><b>为什么 username 要冗余存一份</b>：如果只存 user_id，
 * 用户被删除后就查不出"这条操作是谁干的了"。审计的关键是<b>永久可追溯</b>，
 * 所以要把当时的账号名快照下来。</p>
 *
 * @author CourseHub Team
 */
@Data
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;

    /** 操作人账号（冗余快照，防用户删除后查不出来） */
    private String username;

    /** 操作描述 */
    private String operation;

    private String targetType;
    private Long targetId;

    /** 操作详情 */
    private String detail;

    /** 来源 IP */
    private String ip;

    private LocalDateTime createdAt;
}
