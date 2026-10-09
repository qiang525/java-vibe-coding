package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 提交举报请求参数
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("提交举报参数")
public class ReportCreateDTO {

    @ApiModelProperty(value = "举报对象类型：RESOURCE / COMMENT / USER", required = true)
    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    @ApiModelProperty(value = "举报对象 ID", required = true)
    @NotNull(message = "举报对象不能为空")
    private Long targetId;

    @ApiModelProperty(value = "原因：COPYRIGHT / ERROR / OFFICIAL / AD / SPAM / OTHER", required = true)
    @NotBlank(message = "请选择举报原因")
    private String reason;

    @ApiModelProperty(value = "补充说明")
    @Size(max = 500, message = "补充说明最长 500 个字符")
    private String description;
}
