package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息（返回给前端）
 *
 * <p><b>VO 是什么</b>：专门用来返回给前端的对象。
 * 它和 Entity 最大的区别是——<b>没有 password 字段</b>。</p>
 *
 * <p>这就是"Entity 不能直接返回前端"这条铁律的落地方式：
 * 在 Service 层把 Entity 转成 VO，只挑能给的字段。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("用户信息")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户 ID")
    private Long id;

    @ApiModelProperty("账号")
    private String username;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像 URL")
    private String avatar;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("学号")
    private String studentNo;

    @ApiModelProperty("所属学院 ID")
    private Long collegeId;

    @ApiModelProperty("角色：STUDENT/ASSISTANT/TEACHER/ADMIN")
    private String role;

    @ApiModelProperty("信用分")
    private Integer creditScore;

    @ApiModelProperty("贡献等级")
    private Integer level;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginAt;

    @ApiModelProperty("注册时间")
    private LocalDateTime createdAt;
}
