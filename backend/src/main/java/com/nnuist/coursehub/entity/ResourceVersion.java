package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资料版本实体 —— 对应数据库表 resource_version
 *
 * <p><b>一句话理解这张表</b>：资料的每一次"内容变更"都记一条，
 * 用 {@code parentVersionId} 串起来就成了一棵<b>版本树</b>。</p>
 *
 * <pre>
 *   v1（根，parent = null）
 *    ├── v2          ← 线性修改
 *    │     └── v3
 *    └── v2'         ← 从 v1 分出的另一条分支（branch = "alt"）
 * </pre>
 *
 * <p>本阶段（M2）只做最基础的"新建资料时生成 v1"。
 * 分支、合并、回滚留到 M3 —— 但表结构已经支持了，
 * 到时候不用改表，只加业务逻辑。</p>
 *
 * @author CourseHub Team
 */
@Data
public class ResourceVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 所属资料 ID */
    private Long resourceId;

    /** 该版本对应的文件 ID（指向 file 表） */
    private Long fileId;

    /**
     * 父版本 ID
     *
     * <p>{@code null} 表示这是根版本。这个字段是"树"的关键 ——
     * 每条记录指向自己的父节点，整体就构成树形结构。</p>
     */
    private Long parentVersionId;

    /** 版本号（同一分支内递增，从 1 开始） */
    private Integer versionNo;

    /** 分支名（默认 main） */
    private String branch;

    /** 变更说明（这次改了什么） */
    private String changeLog;

    /** 编辑者 ID */
    private Long editorId;

    /** 是否为回滚产生的版本 */
    private Integer isRollback;

    /** 回滚来源版本 ID（从哪个版本回滚来的） */
    private Long rollbackFromVersionId;

    private LocalDateTime createdAt;
}
