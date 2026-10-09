package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 处理举报请求参数（管理员用）
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("处理举报参数")
public class ReportHandleDTO {

    @ApiModelProperty(value = "处理结果：HANDLED 已处理 / REJECTED 驳回", required = true)
    @NotBlank(message = "请选择处理结果")
    private String status;

    @ApiModelProperty(value = "处理说明")
    private String handleNote;
}
