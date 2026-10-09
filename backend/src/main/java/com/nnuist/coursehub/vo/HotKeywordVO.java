package com.nnuist.coursehub.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 搜索热词
 *
 * <p>数据来自 Redis 的有序集合（ZSet）：每搜索一次就给这个关键词加 1 分，
 * 取分数最高的前 N 个就是"热门搜索词"。</p>
 *
 * @author CourseHub Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("搜索热词")
public class HotKeywordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("关键词")
    private String keyword;

    @ApiModelProperty("被搜索次数")
    private Long count;
}
