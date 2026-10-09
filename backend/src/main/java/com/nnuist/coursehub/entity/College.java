package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学院实体 —— 对应数据库表 sys_college
 *
 * <p>学院是资料组织结构的最上层：<b>学院 → 学期 → 课程 → 章节 → 资料</b>。
 * 有了它，用户才能"先按学院缩小范围，再找课程"。</p>
 *
 * <p>学院是基础字典数据，变动极少，通常由管理员维护。</p>
 *
 * @author CourseHub Team
 */
@Data
public class College implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 学院名称，如"计算机学院" */
    private String name;

    /** 学院代码，如 CS（英文缩写，便于对接其他系统） */
    private String code;

    /** 排序值，越小越靠前（前端下拉框按此排序） */
    private Integer sortOrder;

    private LocalDateTime createdAt;
}
