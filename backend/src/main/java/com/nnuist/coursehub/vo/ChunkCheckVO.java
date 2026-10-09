package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分片上传前的检查结果
 *
 * <p>前端在开始上传前先调这个接口，会拿到三种情况之一：</p>
 *
 * <ol>
 *   <li><b>秒传命中</b>（{@code exists=true}）—— 服务端已有这个文件，
 *       不用传了，直接用返回的 {@code fileId}</li>
 *   <li><b>有历史进度</b>（{@code uploadedChunks} 非空）—— 之前传过一部分，
 *       前端只需要补传没传过的那些分片（<b>断点续传</b>）</li>
 *   <li><b>全新上传</b>（{@code exists=false} 且已传列表为空）—— 从头开始传</li>
 * </ol>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("分片上传检查结果")
public class ChunkCheckVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("是否秒传命中（true 表示服务端已有该文件，无需上传）")
    private Boolean exists;

    @ApiModelProperty("秒传命中时的文件 ID")
    private Long fileId;

    @ApiModelProperty("秒传命中时的原始文件名")
    private String fileName;

    @ApiModelProperty("服务端建议的分片大小（字节）")
    private Long chunkSize;

    @ApiModelProperty("已成功上传的分片索引列表（用于断点续传）")
    private List<Integer> uploadedChunks;

    @ApiModelProperty("已上传分片数量")
    private Integer uploadedCount;
}
