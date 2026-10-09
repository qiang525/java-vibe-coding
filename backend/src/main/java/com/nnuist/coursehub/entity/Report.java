package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报实体 —— 对应数据库表 report
 *
 * <p><b>多态关联</b>：{@code targetType} + {@code targetId} 两个字段
 * 就能指向"资料 / 评论 / 用户"中的任意一种，不用建三张举报表。</p>
 *
 * @author CourseHub Team
 */
@Data
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 举报人 */
    private Long reporterId;

    /** 举报对象类型：RESOURCE / COMMENT / USER */
    private String targetType;

    /** 举报对象 ID */
    private Long targetId;

    /** 原因：COPYRIGHT 侵权 / ERROR 内容错误 / OFFICIAL 官方资料 / AD 广告 / SPAM 垃圾 / OTHER 其他 */
    private String reason;

    private String description;

    /** PENDING 待处理 / HANDLED 已处理 / REJECTED 已驳回 */
    private String status;

    private Long handlerId;
    private String handleNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
