package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 课程新增 / 编辑请求参数
 *
 * <p><b>为什么不用 Entity 直接接收</b>：Entity 里有 {@code ownerId}、
 * {@code memberCount}、{@code status} 这些字段。如果让前端直接传，
 * 用户就能伪造 {@code ownerId} 把课程"送"给别人，或把 {@code memberCount}
 * 改成 9999。所以只开放该让用户填的字段。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("课程新增/编辑参数")
public class CourseSaveDTO {

    @ApiModelProperty(value = "课程名称", required = true, example = "数据结构与算法")
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称最长 100 个字符")
    private String name;

    @ApiModelProperty(value = "课程编号", example = "DS-204")
    @Size(max = 30, message = "课程编号最长 30 个字符")
    private String courseCode;

    @ApiModelProperty(value = "开设学院 ID", example = "1")
    private Long collegeId;

    @ApiModelProperty(value = "授课教师", example = "张老师")
    @Size(max = 50, message = "教师名最长 50 个字符")
    private String teacher;

    @ApiModelProperty(value = "开课学期", example = "2026-2027-1")
    @Size(max = 30, message = "学期最长 30 个字符")
    private String term;

    @ApiModelProperty(value = "学分", example = "3.5")
    @DecimalMin(value = "0.0", message = "学分不能为负")
    @DecimalMax(value = "99.9", message = "学分超出合理范围")
    private BigDecimal credit;

    @ApiModelProperty(value = "课程封面 URL")
    @Size(max = 255, message = "封面地址最长 255 个字符")
    private String cover;

    @ApiModelProperty(value = "课程简介")
    @Size(max = 2000, message = "课程简介最长 2000 个字符")
    private String description;

    @ApiModelProperty(value = "状态：DRAFT/PUBLISHED/ARCHIVED，不传默认 PUBLISHED")
    private String status;
}
