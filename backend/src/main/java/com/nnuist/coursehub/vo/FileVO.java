package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息（返回给前端）
 *
 * <p>注意这里<b>不包含 {@code storagePath}</b> —— 那是服务器上的真实存储路径，
 * 属于内部实现细节，暴露给前端既没用、又给了攻击者线索。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("文件信息")
public class FileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("文件 ID")
    private Long id;

    @ApiModelProperty("原始文件名")
    private String fileName;

    @ApiModelProperty("扩展名")
    private String fileExt;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("MIME 类型")
    private String mimeType;

    @ApiModelProperty("存储类型：LOCAL / MINIO")
    private String storageType;

    @ApiModelProperty("上传者 ID")
    private Long uploaderId;

    @ApiModelProperty("上传时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("下载地址")
    private String downloadUrl;

    @ApiModelProperty("是否命中秒传（true 表示服务端已有该文件，未真正上传）")
    private Boolean instantUpload;

    @ApiModelProperty("易读的文件大小，如 1.2 MB")
    private String readableSize;
}
