package com.nnuist.coursehub.dto;

import com.nnuist.coursehub.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程列表查询条件
 *
 * <p>继承 {@link PageQuery} 后自动获得 page / pageSize / offset，
 * 并自带边界保护（页码不小于 1、每页不超过 100 条）。</p>
 *
 * @author CourseHub Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("课程查询条件")
public class CourseQueryDTO extends PageQuery {

    @ApiModelProperty("关键词：匹配课程名或教师名")
    private String keyword;

    @ApiModelProperty("按学院筛选")
    private Long collegeId;

    @ApiModelProperty("按学期筛选，如 2026-2027-1")
    private String term;

    @ApiModelProperty("按状态筛选：DRAFT/PUBLISHED/ARCHIVED")
    private String status;

    @ApiModelProperty("按负责人筛选（查「我创建的课程」时传当前用户 ID）")
    private Long ownerId;

    @ApiModelProperty("按成员筛选（查「我加入的课程」时传当前用户 ID）")
    private Long memberUserId;
}
