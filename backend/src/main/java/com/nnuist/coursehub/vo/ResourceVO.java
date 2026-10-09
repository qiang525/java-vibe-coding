package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资料信息（返回给前端）
 *
 * <p>这里比 {@code Resource} 实体<b>多了几个字段</b>，都是 SQL JOIN 出来的：</p>
 * <ul>
 *   <li>{@code courseName} —— 课程名（不然前端只拿到 courseId，显示不出来）</li>
 *   <li>{@code uploaderName} —— 上传者昵称</li>
 *   <li>{@code fileName} / {@code fileSize} —— 当前版本对应的文件信息</li>
 *   <li>{@code canEdit} —— 当前用户能不能改这条资料（前端据此显示/隐藏按钮）</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("资料信息")
public class ResourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("资料 ID")
    private Long id;

    @ApiModelProperty("所属课程 ID")
    private Long courseId;

    @ApiModelProperty("所属课程名称（JOIN 带出）")
    private String courseName;

    @ApiModelProperty("所属章节 ID")
    private Long chapterId;

    @ApiModelProperty("上传者 ID")
    private Long uploaderId;

    @ApiModelProperty("上传者昵称（JOIN 带出）")
    private String uploaderName;

    @ApiModelProperty("资料标题")
    private String title;

    @ApiModelProperty("资料描述")
    private String description;

    @ApiModelProperty("类型：TEXTBOOK/LECTURE/EXAM/NOTES/ASSIGNMENT/VIDEO/OTHER")
    private String type;

    @ApiModelProperty("可见性：PUBLIC / COURSE_ONLY / PRIVATE")
    private String visibility;

    @ApiModelProperty("审核状态：PENDING/PUBLISHED/REJECTED/OFFLINE")
    private String status;

    @ApiModelProperty("可信度评分 0-100")
    private BigDecimal score;

    @ApiModelProperty("下载次数")
    private Integer downloadCount;

    @ApiModelProperty("浏览次数")
    private Integer viewCount;

    @ApiModelProperty("收藏次数")
    private Integer favoriteCount;

    @ApiModelProperty("评论数")
    private Integer commentCount;

    @ApiModelProperty("版本数量")
    private Integer versionCount;

    @ApiModelProperty("平均评分 0-5")
    private BigDecimal avgRating;

    @ApiModelProperty("当前版本 ID")
    private Long currentVersionId;

    @ApiModelProperty("文件 ID（当前版本对应的）")
    private Long fileId;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件扩展名")
    private String fileExt;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("文件大小（易读）")
    private String readableSize;

    @ApiModelProperty("文件下载地址")
    private String downloadUrl;

    @ApiModelProperty("当前用户是否有权修改这条资料")
    private Boolean canEdit;

    @ApiModelProperty("搜索相关度（仅搜索接口返回，越大越相关）")
    private Double relevance;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
