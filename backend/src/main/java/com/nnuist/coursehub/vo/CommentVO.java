package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论信息（两级：一级评论里嵌着它的回复列表）
 *
 * @author CourseHub Team
 */
@Data
@ApiModel("评论信息")
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long resourceId;
    private Long userId;

    @ApiModelProperty("评论者昵称")
    private String userName;

    @ApiModelProperty("评论者头像")
    private String userAvatar;

    private Long parentId;
    private Long rootId;

    @ApiModelProperty("类型：NORMAL/ERROR/SUPPLEMENT")
    private String type;

    private String content;

    private Integer likeCount;
    private Integer replyCount;
    private Boolean isTop;

    @ApiModelProperty("当前用户能否删除这条评论")
    private Boolean canDelete;

    private LocalDateTime createdAt;

    @ApiModelProperty("二级回复列表（仅一级评论有）")
    private List<CommentVO> replies;
}
