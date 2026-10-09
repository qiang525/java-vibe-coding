package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.JwtUtil;
import com.nnuist.coursehub.dto.LoginDTO;
import com.nnuist.coursehub.dto.RegisterDTO;
import com.nnuist.coursehub.entity.User;
import com.nnuist.coursehub.mapper.UserMapper;
import com.nnuist.coursehub.service.UserService;
import com.nnuist.coursehub.vo.LoginVO;
import com.nnuist.coursehub.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户业务实现
 *
 * <p>这里放的是<b>真正的业务规则</b>，也是答辩时最值得讲的一层。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * BCrypt 密码编码器
     *
     * <p>用 Spring Security 提供的实现，它自带随机盐，
     * 同一个密码每次加密结果都不同，但都能验证通过。</p>
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 新用户默认信用分 */
    private static final int DEFAULT_CREDIT_SCORE = 100;
    /** 新用户默认等级 */
    private static final int DEFAULT_LEVEL = 1;
    /** 默认角色 */
    private static final String DEFAULT_ROLE = "STUDENT";

    // =========================================================================
    //  登录
    // =========================================================================
    @Override
    public LoginVO login(LoginDTO dto) {
        // ---------- ① 查用户 ----------
        User user = userMapper.selectByUsername(dto.getUsername());

        // 安全要点：用户不存在和密码错误，返回同样的提示
        // 如果提示"用户不存在"，攻击者就能靠它枚举出哪些账号是有效的
        if (user == null) {
            log.warn("登录失败：账号不存在 | username={}", dto.getUsername());
            throw new BizException(ResultCode.LOGIN_FAILED);
        }

        // ---------- ② 校验密码 ----------
        // 注意：不是把数据库密码解密（BCrypt 不可逆），
        // 而是把用户输入的密码用同样的方式加密后再比对
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("登录失败：密码错误 | username={}", dto.getUsername());
            throw new BizException(ResultCode.LOGIN_FAILED);
        }

        // ---------- ③ 检查账号状态 ----------
        if (!"ACTIVE".equals(user.getStatus())) {
            log.warn("登录失败：账号已被禁用 | username={}", dto.getUsername());
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        // ---------- ④ 更新最后登录时间 ----------
        userMapper.updateLastLoginTime(user.getId(), LocalDateTime.now());

        // ---------- ⑤ 签发令牌 ----------
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // ---------- ⑥ 组装返回数据 ----------
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        vo.setUser(toVO(user));

        log.info("登录成功 | userId={} | username={} | role={}",
                user.getId(), user.getUsername(), user.getRole());
        return vo;
    }

    // =========================================================================
    //  注册
    // =========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        // ---------- ① 账号查重 ----------
        if (userMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BizException(ResultCode.ACCOUNT_EXISTS);
        }

        // ---------- ② 组装实体 ----------
        User user = new User();
        user.setUsername(dto.getUsername());
        // 关键：密码必须加密后存储，绝不明文入库
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail() == null ? "" : dto.getEmail());
        user.setStudentNo(dto.getStudentNo() == null ? "" : dto.getStudentNo());
        user.setCollegeId(dto.getCollegeId());
        user.setAvatar("");
        user.setRole(DEFAULT_ROLE);
        user.setCreditScore(DEFAULT_CREDIT_SCORE);
        user.setLevel(DEFAULT_LEVEL);
        user.setStatus("ACTIVE");

        // ---------- ③ 入库 ----------
        userMapper.insert(user);

        log.info("注册成功 | userId={} | username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    // =========================================================================
    //  查询
    // =========================================================================
    @Override
    public UserVO getUserVOById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    // =========================================================================
    //  私有方法
    // =========================================================================

    /**
     * Entity → VO
     *
     * <p>这是"密码不外泄"的最后一道关口。
     * 只把该给的字段复制过去，password 天然被排除。</p>
     */
    private UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        // 用 Spring 的工具按同名属性复制，避免手写十几个 setter
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
