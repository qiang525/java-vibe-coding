package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报信息（管理员处理举报时用）
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("举报信息")
public class ReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long reporterId;

    @ApiModelProperty("举报人昵称")
    private String reporterName;

    @ApiModelProperty("举报对象类型：RESOURCE / COMMENT / USER")
    private String targetType;

    private Long targetId;

    @ApiModelProperty("被举报对象的名字（如资料标题）")
    private String targetTitle;

    @ApiModelProperty("举报原因")
    private String reason;

    private String description;

    @ApiModelProperty("状态：PENDING / HANDLED / REJECTED")
    private String status;

    private Long handlerId;
    private String handlerName;
    private String handleNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
