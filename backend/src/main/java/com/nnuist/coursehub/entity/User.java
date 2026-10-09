package com.nnuist.coursehub.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体 —— 对应数据库表 sys_user
 *
 * <p><b>重要</b>：这个类**绝对不能**直接返回给前端。
 * 因为它含有 {@code password} 字段，直接返回会导致密码哈希泄露。
 * 返回前必须转成 {@link com.nnuist.coursehub.vo.UserVO}。</p>
 *
 * @author CourseHub Team
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 登录账号（学号或自定义） */
    private String username;

    /** 密码（BCrypt 哈希，绝不会是明文） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 学号 */
    private String studentNo;

    /** 所属学院 ID */
    private Long collegeId;

    /** 角色：STUDENT / ASSISTANT / TEACHER / ADMIN */
    private String role;

    /** 信用分（0-100），违规会扣分 */
    private Integer creditScore;

    /** 贡献等级 1-5 */
    private Integer level;

    /** 状态：ACTIVE-正常 / BANNED-已封禁 */
    private String status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 逻辑删除：0-正常 1-已删除 */
    private Integer isDeleted;
}
