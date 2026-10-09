package com.nnuist.coursehub.dto;

import com.nnuist.coursehub.common.page.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资料列表查询条件
 *
 * @author CourseHub Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("资料查询条件")
public class ResourceQueryDTO extends PageQuery {

    @ApiModelProperty("按课程筛选")
    private Long courseId;

    @ApiModelProperty("按上传者筛选")
    private Long uploaderId;

    @ApiModelProperty("按类型筛选")
    private String type;

    @ApiModelProperty("按可见性筛选")
    private String visibility;

    @ApiModelProperty("按状态筛选")
    private String status;

    @ApiModelProperty("关键词：匹配标题或描述")
    private String keyword;

    /**
     * 当前登录用户 ID
     *
     * <p><b>这个字段仅供服务端内部使用，前端传什么都会被覆盖。</b>
     * 它的作用是让 SQL 能按"当前这个人的可见性"过滤：
     * 游客只能看 PUBLIC，课程成员能看 COURSE_ONLY，上传者能看自己的 PRIVATE。</p>
     */
    @ApiModelProperty(value = "当前用户 ID（服务端自动填充，前端无需传）", hidden = true)
    private Long currentUserId;

    /**
     * 当前用户是否管理员
     *
     * <p>管理员可以看到所有资料（不受三级可见性限制），
     * 所以 SQL 里要跳过可见性过滤。同样由服务端填充。</p>
     */
    @ApiModelProperty(value = "是否管理员（服务端自动填充，前端无需传）", hidden = true)
    private Boolean isAdmin;
}
