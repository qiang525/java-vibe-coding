package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资料版本信息
 *
 * <p>版本树的每个节点。{@code parentVersionId} 指向上一个版本，
 * 从根节点往下看就是一条修改链；有分支时则是一棵树。</p>
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("资料版本")
public class VersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("版本 ID")
    private Long id;

    @ApiModelProperty("所属资料 ID")
    private Long resourceId;

    @ApiModelProperty("文件 ID")
    private Long fileId;

    @ApiModelProperty("该版本的文件名")
    private String fileName;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("文件大小（易读）")
    private String readableSize;

    @ApiModelProperty("父版本 ID（null 表示根版本）")
    private Long parentVersionId;

    @ApiModelProperty("版本号")
    private Integer versionNo;

    @ApiModelProperty("分支名")
    private String branch;

    @ApiModelProperty("变更说明")
    private String changeLog;

    @ApiModelProperty("编辑者 ID")
    private Long editorId;

    @ApiModelProperty("编辑者昵称")
    private String editorName;

    @ApiModelProperty("是否为回滚产生的版本")
    private Boolean isRollback;

    @ApiModelProperty("回滚来源版本 ID")
    private Long rollbackFromVersionId;

    @ApiModelProperty("是否是当前生效版本")
    private Boolean isCurrent;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
}
