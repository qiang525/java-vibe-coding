package com.nnuist.coursehub.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏实体 —— 对应数据库表 collection
 *
 * <p>用户与资料的多对多关系，靠唯一索引 {@code uk_user_resource} 防重复收藏。</p>
 *
 * <p><b>⚠️ 为什么必须加 @Alias</b>：MyBatis 内置别名里已经有 "Collection"
 * 指向 {@code java.util.Collection}。如果这里不显式指定别名，
 * 项目启动时会直接报
 * {@code The alias 'Collection' is already mapped to the value 'java.util.Collection'}。
 * 指定成 CollectionEntity 就避开了这个冲突。</p>
 *
 * @author CourseHub Team
 */
@Alias("CollectionEntity")
@Data
public class Collection implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long resourceId;

    /** 收藏夹名称 */
    private String folder;

    private LocalDateTime createdAt;
}
