package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 资料发布 / 编辑请求参数
 *
 * <p><b>为什么要有 {@code fileId}</b>：资料不是"新建"出来的，
 * 而是"把一个已经上传好的文件挂到课程下"。所以流程是两步：</p>
 * <pre>
 *   第一步：POST /api/files/upload   → 拿到 fileId
 *   第二步：POST /api/resources      → 传 fileId + 课程/标题，正式发布
 * </pre>
 *
 * <p>这样设计的好处是：文件上传和资料发布解耦。
 * 大文件可以先传好（甚至分片传），传完再填标题发布。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("资料发布/编辑参数")
public class ResourceSaveDTO {

    @ApiModelProperty(value = "所属课程 ID", required = true, example = "1")
    @NotNull(message = "必须指定所属课程")
    private Long courseId;

    @ApiModelProperty(value = "所属章节 ID（可不填）", example = "1")
    private Long chapterId;

    @ApiModelProperty(value = "文件 ID（先调上传接口拿到）", required = true, example = "12")
    @NotNull(message = "请先上传文件")
    private Long fileId;

    @ApiModelProperty(value = "资料标题", required = true, example = "第二章 线性表 讲义")
    @NotBlank(message = "资料标题不能为空")
    @Size(max = 200, message = "标题最长 200 个字符")
    private String title;

    @ApiModelProperty(value = "资料描述")
    @Size(max = 2000, message = "描述最长 2000 个字符")
    private String description;

    @ApiModelProperty(value = "类型：TEXTBOOK/LECTURE/EXAM/NOTES/ASSIGNMENT/VIDEO/OTHER，默认 OTHER")
    private String type;

    @ApiModelProperty(value = "可见性：PUBLIC/COURSE_ONLY/PRIVATE，默认 PUBLIC")
    private String visibility;
}
