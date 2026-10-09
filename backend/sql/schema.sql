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
