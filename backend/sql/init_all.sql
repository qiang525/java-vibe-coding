-- =============================================================================
--  CourseHub 数据库一键初始化脚本（把三个文件合成一个）
-- -----------------------------------------------------------------------------
--  适用：第一次搭建环境，或者把数据库推倒重来
--  执行：mysql -uroot -p < init_all.sql
--
--  内容顺序：
--    第 1 部分  schema.sql    建库 + 16 张表
--    第 2 部分  init_data.sql 基础数据（学院/账号/课程/章节/标签/通知）
--    第 3 部分  seed_demo.sql 演示课程（竞赛与创新创业指导）
--
--  ⚠️ 注意：本脚本会先删库再建库，已有数据会被清空！
-- =============================================================================

DROP DATABASE IF EXISTS `coursehub`;


-- ============================ 来自 schema.sql ============================

-- =============================================================================
--  CourseHub 课程资料协作平台 - 数据库建表脚本
-- -----------------------------------------------------------------------------
--  数据库 : MySQL 8.0+（必须 8.0 以上，FULLTEXT ngram 中文分词依赖）
--  字符集 : utf8mb4（支持 emoji 和全部中文字符）
--  执行   : mysql -uroot -p < schema.sql
-- -----------------------------------------------------------------------------
--  设计约定：
--    1. 表名/字段名统一小写下划线，不加 tb_ 前缀
--    2. 所有表含 created_at；业务表含 updated_at
--    3. 布尔语义用 tinyint(1)：0=否，1=是
--    4. 枚举值用 varchar + 注释，不用 MySQL ENUM（改值要 DDL，不灵活）
--    5. 软删除 is_deleted：0=正常，1=已删除（核心业务表才有）
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `coursehub`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `coursehub`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
--  一、用户与权限
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`     VARCHAR(50)     NOT NULL COMMENT '登录账号（学号或自定义）',
    `password`     VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt 哈希，绝不存明文）',
    `nickname`     VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`       VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '头像 URL',
    `email`        VARCHAR(100)    NOT NULL DEFAULT '' COMMENT '邮箱',
    `student_no`   VARCHAR(30)     NOT NULL DEFAULT '' COMMENT '学号',
    `college_id`   BIGINT UNSIGNED NULL COMMENT '所属学院ID',
    `role`         VARCHAR(20)     NOT NULL DEFAULT 'STUDENT'
        COMMENT '角色：STUDENT/ASSISTANT/TEACHER/ADMIN',
    `credit_score` INT             NOT NULL DEFAULT 100 COMMENT '信用分（0-100），违规扣分',
    `level`        TINYINT         NOT NULL DEFAULT 1 COMMENT '贡献等级 1-5',
    `status`       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
        COMMENT '状态：ACTIVE-正常 / BANNED-封禁',
    `last_login_at` DATETIME       NULL COMMENT '最后登录时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_college` (`college_id`),
    KEY `idx_role_status` (`role`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';


-- -----------------------------------------------------------------------------
-- 2. 学院表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_college`;
CREATE TABLE `sys_college`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(100)    NOT NULL COMMENT '学院名称',
    `code`       VARCHAR(30)     NOT NULL DEFAULT '' COMMENT '学院代码',
    `sort_order` INT             NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='学院表';


-- =============================================================================
--  二、课程体系
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3. 课程表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `college_id`    BIGINT UNSIGNED NULL COMMENT '开设学院ID',
    `course_code`   VARCHAR(30)     NOT NULL DEFAULT '' COMMENT '课程编号，如 DS-204',
    `name`          VARCHAR(100)    NOT NULL COMMENT '课程名称',
    `teacher`       VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '授课教师',
    `term`          VARCHAR(30)     NOT NULL DEFAULT '' COMMENT '开课学期，如 2026-2027-1',
    `credit`        DECIMAL(3, 1)   NOT NULL DEFAULT 0.0 COMMENT '学分',
    `cover`         VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '课程封面 URL',
    `description`   TEXT            NULL COMMENT '课程简介',
    `owner_id`      BIGINT UNSIGNED NOT NULL COMMENT '创建者/课程负责人ID',
    `member_count`  INT             NOT NULL DEFAULT 0 COMMENT '成员数（冗余字段，列表页展示用）',
    `resource_count` INT            NOT NULL DEFAULT 0 COMMENT '资料数（冗余字段，避免列表页 COUNT）',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'PUBLISHED'
        COMMENT '状态：DRAFT-草稿 / PUBLISHED-已发布 / ARCHIVED-已归档',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_college_term` (`college_id`, `term`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_status` (`status`),
    FULLTEXT KEY `ft_course` (`name`, `description`) WITH PARSER ngram
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程表';


-- -----------------------------------------------------------------------------
-- 4. 课程成员表（多对多：用户 ↔ 课程）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `course_member`;
CREATE TABLE `course_member`
(
    `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `user_id`   BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role`      VARCHAR(20)     NOT NULL DEFAULT 'STUDENT'
        COMMENT '课程内角色：OWNER-负责人 / ASSISTANT-助教 / STUDENT-学生',
    `joined_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_user` (`course_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程成员表';


-- -----------------------------------------------------------------------------
-- 5. 章节表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `chapter`;
CREATE TABLE `chapter`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属课程ID',
    `name`       VARCHAR(100)    NOT NULL COMMENT '章节名称',
    `sort_order` INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_course_sort` (`course_id`, `sort_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程章节表';


-- =============================================================================
--  三、文件与资料
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 6. 文件表（物理文件与业务解耦）
--    设计要点：同一份文件可能被多份资料引用，独立存储 + 引用计数
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_hash`    CHAR(32)        NOT NULL COMMENT '文件 MD5 哈希（秒传依据）',
    `file_name`    VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    `file_ext`     VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '扩展名（小写）',
    `storage_path` VARCHAR(500)    NOT NULL COMMENT '存储路径（相对路径）',
    `file_size`    BIGINT          NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `mime_type`    VARCHAR(100)    NOT NULL DEFAULT '' COMMENT 'MIME 类型',
    `storage_type` VARCHAR(20)     NOT NULL DEFAULT 'LOCAL'
        COMMENT '存储类型：LOCAL-本地 / MINIO-对象存储',
    `ref_count`    INT             NOT NULL DEFAULT 0 COMMENT '被引用次数，归零才可清理',
    `uploader_id`  BIGINT UNSIGNED NOT NULL COMMENT '上传者ID',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_hash` (`file_hash`),
    KEY `idx_ref_count` (`ref_count`),
    KEY `idx_uploader` (`uploader_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文件表';


-- -----------------------------------------------------------------------------
-- 7. 资料表（核心业务表）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `resource`;
CREATE TABLE `resource`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id`       BIGINT UNSIGNED NOT NULL COMMENT '所属课程ID',
    `chapter_id`      BIGINT UNSIGNED NULL COMMENT '所属章节ID（可为空）',
    `uploader_id`     BIGINT UNSIGNED NOT NULL COMMENT '上传者ID',
    `current_version_id` BIGINT UNSIGNED NULL COMMENT '当前版本ID',
    `title`           VARCHAR(200)    NOT NULL COMMENT '资料标题',
    `description`     TEXT            NULL COMMENT '资料描述',
    `type`            VARCHAR(30)     NOT NULL DEFAULT 'OTHER'
        COMMENT '资料类型：TEXTBOOK-教材 / LECTURE-讲义 / EXAM-真题 / NOTES-笔记 / ASSIGNMENT-作业 / VIDEO-视频 / OTHER-其他',
    `visibility`      VARCHAR(20)     NOT NULL DEFAULT 'PUBLIC'
        COMMENT '可见性：PUBLIC-全平台 / COURSE_ONLY-仅本课程 / PRIVATE-仅自己',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        COMMENT '状态：PENDING-待审核 / PUBLISHED-已发布 / REJECTED-已驳回 / OFFLINE-已下架',
    `score`           DECIMAL(5, 2)   NOT NULL DEFAULT 0.00 COMMENT '可信度评分 0-100（定时重算）',
    `download_count`  INT             NOT NULL DEFAULT 0 COMMENT '下载次数',
    `view_count`      INT             NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `favorite_count`  INT             NOT NULL DEFAULT 0 COMMENT '收藏次数',
    `comment_count`   INT             NOT NULL DEFAULT 0 COMMENT '评论数',
    `version_count`   INT             NOT NULL DEFAULT 1 COMMENT '版本数量',
    `avg_rating`      DECIMAL(2, 1)   NOT NULL DEFAULT 0.0 COMMENT '平均评分 0-5',
    `rating_count`    INT             NOT NULL DEFAULT 0 COMMENT '评分人数',
    `reject_reason`   VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '驳回原因',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    -- 列表页高频查询：某课程下已发布资料，按可信度降序（最左前缀原则）
    KEY `idx_course_status_score` (`course_id`, `status`, `score` DESC),
    KEY `idx_chapter` (`chapter_id`),
    KEY `idx_uploader` (`uploader_id`),
    KEY `idx_status_created` (`status`, `created_at` DESC),
    KEY `idx_visibility` (`visibility`),
    FULLTEXT KEY `ft_resource` (`title`, `description`) WITH PARSER ngram
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='资料表';


-- -----------------------------------------------------------------------------
-- 8. 资料版本表（树形结构，支持分支与回滚）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `resource_version`;
CREATE TABLE `resource_version`
(
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_id`            BIGINT UNSIGNED NOT NULL COMMENT '所属资料ID',
    `file_id`                BIGINT UNSIGNED NOT NULL COMMENT '该版本对应的文件ID',
    `parent_version_id`      BIGINT UNSIGNED NULL COMMENT '父版本ID（NULL=根版本，构成版本树）',
    `version_no`             INT             NOT NULL DEFAULT 1 COMMENT '版本号（分支内递增）',
    `branch`                 VARCHAR(50)     NOT NULL DEFAULT 'main' COMMENT '分支名',
    `change_log`             VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '变更说明',
    `editor_id`              BIGINT UNSIGNED NOT NULL COMMENT '编辑者ID',
    `is_rollback`            TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否为回滚产生的版本',
    `rollback_from_version_id` BIGINT UNSIGNED NULL COMMENT '回滚来源版本ID',
    `created_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_resource_branch` (`resource_id`, `branch`, `version_no`),
    KEY `idx_parent` (`parent_version_id`),
    KEY `idx_file` (`file_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='资料版本表';


-- =============================================================================
--  四、互动
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 9. 评论表（两级结构：一级评论 + 二级回复）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_id` BIGINT UNSIGNED NOT NULL COMMENT '资料ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '评论者ID',
    `parent_id`   BIGINT UNSIGNED NULL COMMENT '父评论ID（NULL=一级评论）',
    `root_id`     BIGINT UNSIGNED NULL COMMENT '根评论ID（楼中楼聚合用）',
    `type`        VARCHAR(20)     NOT NULL DEFAULT 'NORMAL'
        COMMENT '类型：NORMAL-讨论 / ERROR-纠错 / SUPPLEMENT-补充 / REPORT-举报可疑',
    `content`     TEXT            NOT NULL COMMENT '评论内容',
    `like_count`  INT             NOT NULL DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT             NOT NULL DEFAULT 0 COMMENT '回复数',
    `is_top`      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否置顶',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/HIDDEN',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_resource_created` (`resource_id`, `created_at` DESC),
    KEY `idx_root` (`root_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='评论表';


-- -----------------------------------------------------------------------------
-- 10. 收藏表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `collection`;
CREATE TABLE `collection`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `resource_id` BIGINT UNSIGNED NOT NULL COMMENT '资料ID',
    `folder`      VARCHAR(50)     NOT NULL DEFAULT '默认收藏夹' COMMENT '收藏夹名称',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_resource` (`user_id`, `resource_id`),
    KEY `idx_resource` (`resource_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收藏表';


-- -----------------------------------------------------------------------------
-- 11. 下载记录表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `download_record`;
CREATE TABLE `download_record`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_id` BIGINT UNSIGNED NOT NULL COMMENT '资料ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '下载者ID',
    `ip`          VARCHAR(50)     NOT NULL DEFAULT '' COMMENT 'IP 地址',
    `user_agent`  VARCHAR(500)    NOT NULL DEFAULT '' COMMENT 'UA',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
    PRIMARY KEY (`id`),
    KEY `idx_resource` (`resource_id`),
    KEY `idx_user_created` (`user_id`, `created_at` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='下载记录表';


-- -----------------------------------------------------------------------------
-- 12. 举报表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `reporter_id`  BIGINT UNSIGNED NOT NULL COMMENT '举报人ID',
    `target_type`  VARCHAR(20)     NOT NULL COMMENT '举报对象类型：RESOURCE/COMMENT/USER',
    `target_id`    BIGINT UNSIGNED NOT NULL COMMENT '举报对象ID',
    `reason`       VARCHAR(30)     NOT NULL
        COMMENT '原因：COPYRIGHT-侵权 / ERROR-内容错误 / OFFICIAL-官方资料 / AD-广告 / SPAM-垃圾 / OTHER-其他',
    `description`  VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '补充说明',
    `status`       VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        COMMENT '状态：PENDING-待处理 / HANDLED-已处理 / REJECTED-已驳回',
    `handler_id`   BIGINT UNSIGNED NULL COMMENT '处理人ID',
    `handle_note`  VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '处理说明',
    `handled_at`   DATETIME        NULL COMMENT '处理时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='举报表';


-- -----------------------------------------------------------------------------
-- 13. 标签表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)     NOT NULL COMMENT '标签名',
    `usage_count` INT             NOT NULL DEFAULT 0 COMMENT '使用次数（用于热门标签）',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_usage` (`usage_count` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='标签表';


-- -----------------------------------------------------------------------------
-- 14. 资料标签关联表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `resource_tag`;
CREATE TABLE `resource_tag`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_id` BIGINT UNSIGNED NOT NULL COMMENT '资料ID',
    `tag_id`      BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_tag` (`resource_id`, `tag_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='资料标签关联表';


-- -----------------------------------------------------------------------------
-- 15. 通知表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    `type`        VARCHAR(30)     NOT NULL COMMENT '通知类型',
    `title`       VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '标题',
    `content`     VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '内容',
    `target_type` VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '跳转目标类型',
    `target_id`   BIGINT UNSIGNED NULL COMMENT '跳转目标ID',
    `is_read`     TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已读',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read_created` (`user_id`, `is_read`, `created_at` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知表';


-- -----------------------------------------------------------------------------
-- 16. 操作审计日志表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NULL COMMENT '操作人ID',
    `username`    VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '操作人账号（冗余，防用户删除）',
    `operation`   VARCHAR(100)    NOT NULL COMMENT '操作描述',
    `target_type` VARCHAR(30)     NOT NULL DEFAULT '' COMMENT '目标类型',
    `target_id`   BIGINT UNSIGNED NULL COMMENT '目标ID',
    `detail`      VARCHAR(1000)   NOT NULL DEFAULT '' COMMENT '操作详情',
    `ip`          VARCHAR(50)     NOT NULL DEFAULT '' COMMENT 'IP',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_created` (`created_at` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作审计日志表';


SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
--  建表完成，共 16 张表
--  种子数据见 init_data.sql
-- =============================================================================

-- ============================ 来自 init_data.sql ============================

-- =============================================================================
--  CourseHub 初始化数据
-- -----------------------------------------------------------------------------
--  执行前请先执行 schema.sql
--  执行：mysql -uroot -p --default-character-set=utf8mb4 coursehub < init_data.sql
--
--  测试账号（密码为 BCrypt 加密存储，数据库里看不到明文）：
--    管理员   admin        / admin123
--    教师     teacher01    / 123456
--    学生     2025210101   / 123456
--
--  ⚠️ 部署到公网前必须修改这些默认密码
-- =============================================================================

USE `coursehub`;

SET NAMES utf8mb4;

-- =============================================================================
--  1. 学院
-- =============================================================================
INSERT INTO `sys_college` (`id`, `name`, `code`, `sort_order`) VALUES
(1, '计算机学院', 'CS', 1),
(2, '大气科学学院', 'AS', 2),
(3, '遥感与测绘工程学院', 'RS', 3),
(4, '电子与信息工程学院', 'EI', 4),
(5, '数学与统计学院', 'MATH', 5)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- =============================================================================
--  2. 用户
--     密码为 BCrypt 哈希，已通过 encoder.matches() 验证可用
-- =============================================================================
INSERT INTO `sys_user`
(`id`, `username`, `password`, `nickname`, `email`, `student_no`, `college_id`, `role`, `credit_score`, `level`)
VALUES
(1, 'admin', '$2a$10$OGSdL9q1L.uBf7RYvH4DXOl1fvNPOhLXO.0EVSMWKss9qS1kNms72',
 '系统管理员', 'admin@coursehub.local', 'ADMIN', 1, 'ADMIN', 100, 5),
(2, 'teacher01', '$2a$10$Kqek/eaVmHQbMxzXIFh8leVMJItRz1RfOEWRMjcXXY/gieoCgCyLW',
 '张老师', 'zhang@nnuist.edu.cn', 'T2026001', 1, 'TEACHER', 100, 5),
(3, '2025210101', '$2a$10$F0pe74b.UD2MNzuS15egW.2tMb6/dnNe9SeZNC/GN1ekJQ/Ykxopm',
 '李同学', 'stu001@nnuist.edu.cn', '2025210101', 1, 'STUDENT', 100, 2)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

-- =============================================================================
--  3. 课程
-- =============================================================================
INSERT INTO `course`
(`id`, `college_id`, `course_code`, `name`, `teacher`, `term`, `credit`, `description`, `owner_id`, `member_count`, `resource_count`, `status`)
VALUES
(1, 1, 'DS-204', '数据结构与算法', '张老师', '2026-2027-1', 4.0,
 '讲授线性表、栈、队列、树、图等基本数据结构，以及排序、查找等经典算法。课程设计强调动手实现与复杂度分析。',
 2, 3, 0, 'PUBLISHED'),
(2, 1, 'CP-301', '编译原理', '李老师', '2026-2027-1', 3.5,
 '涵盖词法分析、语法分析、语义分析、中间代码生成与目标代码优化等编译全过程。',
 2, 2, 0, 'PUBLISHED'),
(3, 1, 'OS-302', '操作系统', '王老师', '2026-2027-1', 4.0,
 '进程管理、内存管理、文件系统、设备管理。重点理解并发与死锁。',
 2, 1, 0, 'PUBLISHED'),
(4, 2, 'AS-101', '大气科学导论', '陈老师', '2026-2027-1', 3.0,
 '大气组成与结构、辐射传输、热力学基础、大气环流概述。',
 2, 1, 0, 'PUBLISHED')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- =============================================================================
--  4. 课程成员
-- =============================================================================
INSERT INTO `course_member` (`course_id`, `user_id`, `role`) VALUES
(1, 2, 'OWNER'),
(1, 3, 'STUDENT'),
(1, 1, 'ASSISTANT'),
(2, 2, 'OWNER'),
(2, 3, 'STUDENT'),
(3, 2, 'OWNER'),
(4, 2, 'OWNER')
ON DUPLICATE KEY UPDATE `role` = VALUES(`role`);

-- =============================================================================
--  5. 章节
-- =============================================================================
INSERT INTO `chapter` (`id`, `course_id`, `name`, `sort_order`) VALUES
(1, 1, '第一章 绪论与复杂度分析', 1),
(2, 1, '第二章 线性表', 2),
(3, 1, '第三章 栈与队列', 3),
(4, 1, '第四章 树与二叉树', 4),
(5, 1, '第五章 图', 5),
(6, 1, '第六章 查找', 6),
(7, 1, '第七章 排序', 7),
(8, 2, '第一章 引论', 1),
(9, 2, '第二章 词法分析', 2),
(10, 2, '第三章 语法分析', 3)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- =============================================================================
--  6. 标签
-- =============================================================================
INSERT INTO `tag` (`id`, `name`, `usage_count`) VALUES
(1, '期末复习', 0),
(2, '真题', 0),
(3, '课件', 0),
(4, '笔记', 0),
(5, '习题答案', 0),
(6, '课程设计', 0),
(7, '考研', 0),
(8, '实验报告', 0)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- =============================================================================
--  7. 通知（示例）
-- =============================================================================
INSERT INTO `notification` (`user_id`, `type`, `title`, `content`, `target_type`, `target_id`) VALUES
(3, 'SYSTEM', '欢迎使用 CourseHub', '你可以按课程浏览资料，也可以上传自己整理的笔记帮助同学。', '', NULL),
(3, 'COURSE_JOINED', '你已加入《数据结构与算法》', '该课程共有 3 名成员，快去看看吧。', 'COURSE', 1);

-- =============================================================================
--  完成
-- -----------------------------------------------------------------------------
--  说明：资源（resource）与文件（file）表暂不插入数据，
--        因为需要真实的物理文件支撑。等 M2 阶段实现上传功能后，
--        通过前端界面上传即可自动生成。
-- =============================================================================

-- ============================ 来自 seed_demo.sql ============================

-- =============================================================================
--  CourseHub 演示课程补充脚本（可选执行）
-- -----------------------------------------------------------------------------
--  作用：在 schema.sql + init_data.sql 之后执行，补一门"竞赛与创新创业指导"课
--        并挂上三个章节，方便你上传资料时有现成的归属课程
--
--  说明：资料（PDF/PPT）无法用 SQL 造出来，必须走页面上传，所以这里只准备课程骨架
--
--  执行方式（任选其一）：
--    1. 命令行：mysql -uroot -p < seed_demo.sql
--    2. Navicat / IDEA Database 面板里直接运行本文件
-- =============================================================================

USE `coursehub`;

-- -----------------------------------------------------------------------------
-- 1. 课程：竞赛与创新创业指导
-- -----------------------------------------------------------------------------
INSERT INTO `course`
(`id`, `course_code`, `name`, `college_id`, `teacher`, `term`, `credit`,
 `cover`, `description`, `owner_id`, `member_count`, `resource_count`, `status`)
VALUES
(5, 'CX-101', '竞赛与创新创业指导', 3, '陈老师', '2026-2027-1', 1.0,
 '', '汇总学科竞赛目录、申报流程与材料模板，适合作为课外实践与竞赛规划的参考资料。',
 2, 3, 0, 'PUBLISHED')
ON DUPLICATE KEY UPDATE
  `name`        = VALUES(`name`),
  `teacher`     = VALUES(`teacher`),
  `description` = VALUES(`description`);

-- -----------------------------------------------------------------------------
-- 2. 课程章节（重复执行会先清空再插入，保证幂等）
-- -----------------------------------------------------------------------------
DELETE FROM `chapter` WHERE `course_id` = 5;

INSERT INTO `chapter` (`course_id`, `name`, `sort_order`) VALUES
(5, '第一章 竞赛政策与目录解读', 1),
(5, '第二章 申报材料写作规范', 2),
(5, '第三章 常用模板与范例', 3);

-- -----------------------------------------------------------------------------
-- 3. 课程成员（老师是课程创建者，学生已加入）
-- -----------------------------------------------------------------------------
DELETE FROM `course_member` WHERE `course_id` = 5;

INSERT INTO `course_member` (`course_id`, `user_id`, `role`) VALUES
(5, 2, 'OWNER'),
(5, 3, 'STUDENT');

-- -----------------------------------------------------------------------------
-- 4. 修正课程统计字段（成员数按实际数量回填）
-- -----------------------------------------------------------------------------
UPDATE `course` c
SET c.`member_count`   = (SELECT COUNT(*) FROM `course_member` m WHERE m.`course_id` = c.`id`),
    c.`resource_count` = (SELECT COUNT(*) FROM `resource` r WHERE r.`course_id` = c.`id` AND r.`is_deleted` = 0);
