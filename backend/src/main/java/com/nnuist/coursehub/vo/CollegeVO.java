package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 学院信息（返回给前端）
 *
 * <p>用于登录页/注册页的"学院"下拉框，以及课程列表页的筛选条件。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("学院信息")
public class CollegeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("学院 ID")
    private Long id;

    @ApiModelProperty("学院名称")
    private String name;

    @ApiModelProperty("学院代码")
    private String code;
}
