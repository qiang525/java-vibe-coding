package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资料实体 —— 对应数据库表 resource
 *
 * <p><b>这是整个系统最核心的一张表</b>。一份"资料"就是一条记录，
 * 它把「课程」「上传者」「文件」三者串了起来。</p>
 *
 * <pre>
 *   course（课程）
 *      └── resource（资料）          ← 你在这里
 *              ├── resource_version（版本）→ file（物理文件）
 *              ├── comment（评论）
 *              └── collection（收藏）
 * </pre>
 *
 * <p><b>注意 {@code currentVersionId} 这个字段</b>：资料本身不直接存文件 ID，
 * 而是通过"当前版本"间接指向文件。这样做是为了支持版本树——
 * 每次修改生成一个新版本，把 {@code currentVersionId} 指过去即可，
 * 历史版本一条都不会丢。</p>
 *
 * @author CourseHub Team
 */
@Data
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 所属课程 ID */
    private Long courseId;

    /** 所属章节 ID（可为空，表示未归类到具体章节） */
    private Long chapterId;

    /** 上传者 ID */
    private Long uploaderId;

    /**
     * 当前版本 ID（指向 resource_version.id）
     *
     * <p>新增资料时会先插入 resource，再插入一条 v1 版本，
     * 最后把这个字段更新为 v1 的 ID。</p>
     */
    private Long currentVersionId;

    /** 资料标题 */
    private String title;

    /** 资料描述 */
    private String description;

    /** 资料类型：TEXTBOOK 教材 / LECTURE 讲义 / EXAM 真题 / NOTES 笔记 / ASSIGNMENT 作业 / VIDEO 视频 / OTHER 其他 */
    private String type;

    /**
     * 三级可见性 —— <b>本模块最重要的字段</b>
     *
     * <ul>
     *   <li>{@code PUBLIC} —— 全平台可见（默认）</li>
     *   <li>{@code COURSE_ONLY} —— 仅本课程成员可见</li>
     *   <li>{@code PRIVATE} —— 仅上传者自己可见</li>
     * </ul>
     *
     * <p>它管的是"<b>这条数据你能不能看</b>"，属于<b>数据权限</b>；
     * 而拦截器管的是"<b>这个接口你能不能调</b>"，属于功能权限。
     * 两者是不同维度，缺一不可。</p>
     */
    private String visibility;

    /** 审核状态：PENDING 待审核 / PUBLISHED 已发布 / REJECTED 已驳回 / OFFLINE 已下架 */
    private String status;

    /** 可信度评分 0-100（M4 定时重算） */
    private BigDecimal score;

    /** 下载次数 */
    private Integer downloadCount;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏次数 */
    private Integer favoriteCount;

    /** 评论数 */
    private Integer commentCount;

    /** 版本数量 */
    private Integer versionCount;

    /** 平均评分 0-5 */
    private BigDecimal avgRating;

    /** 评分人数 */
    private Integer ratingCount;

    /** 驳回原因（审核不通过时给用户看） */
    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 逻辑删除：0-正常 1-已删除 */
    private Integer isDeleted;
}
