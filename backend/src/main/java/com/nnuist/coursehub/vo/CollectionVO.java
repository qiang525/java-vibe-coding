package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏信息（含资料的摘要信息，前端可直接渲染列表）
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("收藏信息")
public class CollectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("收藏记录 ID")
    private Long id;

    private Long resourceId;

    @ApiModelProperty("资料标题")
    private String title;

    @ApiModelProperty("资料类型")
    private String type;

    @ApiModelProperty("可见性")
    private String visibility;

    @ApiModelProperty("课程名")
    private String courseName;

    @ApiModelProperty("上传者昵称")
    private String uploaderName;

    @ApiModelProperty("文件名")
    private String fileName;

    private Long fileSize;

    @ApiModelProperty("文件大小（易读）")
    private String readableSize;

    private Integer downloadCount;

    @ApiModelProperty("收藏夹")
    private String folder;

    @ApiModelProperty("收藏时间")
    private LocalDateTime createdAt;
}
