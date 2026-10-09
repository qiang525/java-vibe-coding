package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新增版本请求参数
 *
 * <p>场景：资料已经发布了 v1，现在想更新内容 —— 上传新文件后调这个接口，
 * 会生成 v2 并把资料的"当前版本指针"指过去，v1 完整保留。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("新增版本参数")
public class VersionCreateDTO {

    @ApiModelProperty(value = "新文件 ID（先调上传接口拿到）", required = true)
    @NotNull(message = "请先上传新版本文件")
    private Long fileId;

    @ApiModelProperty(value = "变更说明", example = "修正了第三章的公式错误")
    @Size(max = 500, message = "变更说明最长 500 个字符")
    private String changeLog;

    @ApiModelProperty(value = "基于哪个版本修改（不传则基于当前版本）")
    private Long parentVersionId;
}
