package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录请求参数
 *
 * <p><b>DTO 是什么</b>：专门用来接收前端传参的对象。
 * 和 Entity 分开，是为了只暴露"该让前端传的字段"——
 * 前端不该传 `role`，否则就能自己把自己设成管理员了。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("登录请求参数")
public class LoginDTO {

    @ApiModelProperty(value = "账号（学号或用户名）", required = true, example = "admin")
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度须为 3-50 个字符")
    private String username;

    @ApiModelProperty(value = "密码", required = true, example = "admin123")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度须为 6-50 个字符")
    private String password;
}
