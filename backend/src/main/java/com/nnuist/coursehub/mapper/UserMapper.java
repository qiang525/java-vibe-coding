package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 用户数据访问接口
 *
 * <p><b>这一层只做一件事</b>：执行 SQL、把结果映射成 Java 对象。
 * 不写任何业务判断（比如"密码对不对"属于业务，不在这层）。</p>
 *
 * <p>真正的 SQL 写在 {@code resources/mapper/UserMapper.xml} 里。</p>
 *
 * @author CourseHub Team
 */
@Mapper
public interface UserMapper {

    /**
     * 按账号查询用户（登录时用）
     *
     * <p>对应索引：{@code uk_username}，走唯一索引，速度极快。</p>
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 按主键查询用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 新增用户，返回受影响行数
     *
     * <p>插入后 {@code user.id} 会被自动回填（useGeneratedKeys）。</p>
     */
    int insert(User user);

    /**
     * 更新最后登录时间
     */
    int updateLastLoginTime(@Param("id") Long id,
                            @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * 统计某账号是否存在（注册时查重用）
     */
    int countByUsername(@Param("username") String username);
}
