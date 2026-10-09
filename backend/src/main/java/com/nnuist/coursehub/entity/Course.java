package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体 —— 对应数据库表 course
 *
 * <p><b>课程在系统中的位置</b>：它是"资料的容器"。
 * 每份资料都必须挂在某个课程下，这样用户才能按课程找到需要的资料。
 * 课程上承学院（college_id）、下挂资料（resource.course_id）。</p>
 *
 * <p><b>两个冗余字段的设计意图</b>：</p>
 * <ul>
 *   <li>{@code memberCount} 成员数</li>
 *   <li>{@code resourceCount} 资料数</li>
 * </ul>
 * <p>这两个数字理论上可以每次现算（COUNT 查询），但课程列表页有几十条课程，
 * 每条都 COUNT 一次就是典型的<b>N+1 查询</b>，会把数据库拖垮。
 * 所以这里用"冗余字段 + 变更时维护"的方式，列表页直接读字段，速度快得多。</p>
 *
 * @author CourseHub Team
 */
@Data
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 开设学院 ID（关联 sys_college.id） */
    private Long collegeId;

    /** 课程编号，如 DS-204 */
    private String courseCode;

    /** 课程名称，如"数据结构与算法" */
    private String name;

    /** 授课教师 */
    private String teacher;

    /** 开课学期，如 2026-2027-1 */
    private String term;

    /** 学分，DECIMAL(3,1)，如 3.5 */
    private BigDecimal credit;

    /** 课程封面 URL */
    private String cover;

    /** 课程简介 */
    private String description;

    /** 创建者/课程负责人 ID（用于数据权限判断：只有他能改这门课） */
    private Long ownerId;

    /** 成员数（冗余字段，列表页直接读取） */
    private Integer memberCount;

    /** 资料数（冗余字段，列表页直接读取） */
    private Integer resourceCount;

    /** 状态：DRAFT-草稿 / PUBLISHED-已发布 / ARCHIVED-已归档 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 逻辑删除：0-正常 1-已删除 */
    private Integer isDeleted;
}
