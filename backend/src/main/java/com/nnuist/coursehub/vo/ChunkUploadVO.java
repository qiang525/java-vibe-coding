package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 单个分片的上传结果
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("分片上传结果")
public class ChunkUploadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("本次上传的分片索引")
    private Integer chunkIndex;

    @ApiModelProperty("累计已上传分片数")
    private Integer uploadedCount;

    @ApiModelProperty("总分片数")
    private Integer totalChunks;

    @ApiModelProperty("是否已全部传完（true 时前端应调用合并接口）")
    private Boolean finished;

    public ChunkUploadVO(Integer chunkIndex, Integer uploadedCount, Integer totalChunks) {
        this.chunkIndex = chunkIndex;
        this.uploadedCount = uploadedCount;
        this.totalChunks = totalChunks;
        this.finished = uploadedCount != null && totalChunks != null
                && uploadedCount >= totalChunks;
    }
}
