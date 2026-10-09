package com.nnuist.coursehub.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 分片合并请求参数
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("分片合并参数")
public class ChunkMergeDTO {

    @ApiModelProperty(value = "全文件 MD5（32 位十六进制）", required = true,
            example = "d41d8cd98f00b204e9800998ecf8427e")
    @NotBlank(message = "文件哈希不能为空")
    @Pattern(regexp = "^[a-fA-F0-9]{32}$", message = "文件哈希格式不正确")
    private String fileHash;

    @ApiModelProperty(value = "原始文件名（含扩展名）", required = true, example = "课件.pptx")
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @ApiModelProperty(value = "文件总大小（字节）", required = true, example = "50280605")
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小不合法")
    private Long fileSize;

    @ApiModelProperty(value = "分片总数", required = true, example = "10")
    @NotNull(message = "分片总数不能为空")
    @Min(value = 1, message = "分片总数不合法")
    private Integer totalChunks;
}
