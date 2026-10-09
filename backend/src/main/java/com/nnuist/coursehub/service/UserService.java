package com.nnuist.coursehub.service;

import com.nnuist.coursehub.dto.LoginDTO;
import com.nnuist.coursehub.dto.RegisterDTO;
import com.nnuist.coursehub.vo.LoginVO;
import com.nnuist.coursehub.vo.UserVO;

/**
 * 用户业务接口
 *
 * <p><b>为什么要定义接口再写实现</b>：
 * Controller 只依赖这个接口，不关心具体怎么实现。
 * 将来要换实现（比如改成对接学校统一认证），Controller 代码一行都不用改。</p>
 *
 * @author CourseHub Team
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param dto 登录参数
     * @return 令牌 + 用户信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户注册
     *
     * @param dto 注册参数
     * @return 新用户的 ID
     */
    Long register(RegisterDTO dto);

    /**
     * 按 ID 查询用户信息（返回 VO，不含密码）
     */
    UserVO getUserVOById(Long id);
}
