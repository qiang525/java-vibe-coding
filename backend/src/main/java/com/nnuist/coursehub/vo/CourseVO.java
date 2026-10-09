package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程信息（返回给前端）
 *
 * <p><b>注意 {@code collegeName} 这个字段</b>：数据库里课程只存
 * {@code college_id}，前端展示却要显示"计算机学院"。
 * 有两种做法：</p>
 * <ul>
 *   <li>前端拿 college_id 去学院列表里查 —— 要在前端维护一份映射，麻烦</li>
 *   <li>✅ 后端 SQL 用 JOIN 一次把这些字段带出来 —— 前端拿到就能直接显示</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("课程信息")
public class CourseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("课程 ID")
    private Long id;

    @ApiModelProperty("课程编号")
    private String courseCode;

    @ApiModelProperty("课程名称")
    private String name;

    @ApiModelProperty("所属学院 ID")
    private Long collegeId;

    @ApiModelProperty("所属学院名称（由 SQL JOIN 带出）")
    private String collegeName;

    @ApiModelProperty("授课教师")
    private String teacher;

    @ApiModelProperty("开课学期")
    private String term;

    @ApiModelProperty("学分")
    private BigDecimal credit;

    @ApiModelProperty("课程封面")
    private String cover;

    @ApiModelProperty("课程简介")
    private String description;

    @ApiModelProperty("负责人 ID")
    private Long ownerId;

    @ApiModelProperty("成员数")
    private Integer memberCount;

    @ApiModelProperty("资料数")
    private Integer resourceCount;

    @ApiModelProperty("状态：DRAFT/PUBLISHED/ARCHIVED")
    private String status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
}
