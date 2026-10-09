package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发表评论请求参数
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("发表评论参数")
public class CommentCreateDTO {

    @ApiModelProperty(value = "评论内容", required = true)
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容最长 1000 个字符")
    private String content;

    @ApiModelProperty(value = "父评论 ID（回复某条评论时传）")
    private Long parentId;

    @ApiModelProperty(value = "类型：NORMAL 讨论 / ERROR 纠错 / SUPPLEMENT 补充，默认 NORMAL")
    private String type;
}
