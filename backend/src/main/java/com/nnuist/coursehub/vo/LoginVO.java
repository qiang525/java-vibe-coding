package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回的数据
 *
 * <p>前端拿到后要做三件事：</p>
 * <ol>
 *   <li>把 {@code token} 存进 localStorage</li>
 *   <li>把 {@code user} 存进 Pinia，供各页面读取</li>
 *   <li>根据 {@code user.role} 决定显示哪些菜单</li>
 * </ol>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("登录结果")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("登录令牌，前端需在后续请求的 Authorization 头中携带")
    private String token;

    @ApiModelProperty("令牌类型，固定为 Bearer")
    private String tokenType = "Bearer";

    @ApiModelProperty("有效期（秒）")
    private Long expiresIn;

    @ApiModelProperty("当前登录用户信息")
    private UserVO user;
}
