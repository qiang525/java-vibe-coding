package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册请求参数
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("注册请求参数")
public class RegisterDTO {

    @ApiModelProperty(value = "账号（学号或自定义）", required = true, example = "2025210199")
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度须为 3-50 个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String username;

    @ApiModelProperty(value = "密码", required = true, example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度须为 6-50 个字符")
    private String password;

    @ApiModelProperty(value = "昵称", required = true, example = "王同学")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称不能超过 50 个字符")
    private String nickname;

    @ApiModelProperty(value = "学号", example = "2025210199")
    @Size(max = 30, message = "学号不能超过 30 个字符")
    private String studentNo;

    @ApiModelProperty(value = "邮箱", example = "wang@nnuist.edu.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

    @ApiModelProperty(value = "学院 ID", example = "1")
    private Long collegeId;
}
