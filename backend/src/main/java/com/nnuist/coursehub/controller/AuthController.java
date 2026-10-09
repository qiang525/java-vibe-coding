package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.annotation.RequireLogin;
import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.dto.LoginDTO;
import com.nnuist.coursehub.dto.RegisterDTO;
import com.nnuist.coursehub.service.UserService;
import com.nnuist.coursehub.vo.LoginVO;
import com.nnuist.coursehub.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证与用户接口
 *
 * <p><b>Controller 的职责边界</b>：</p>
 * <ul>
 *   <li>✅ 接收参数、触发校验（{@code @Valid}）</li>
 *   <li>✅ 调用 Service</li>
 *   <li>✅ 包装成统一响应体返回</li>
 *   <li>❌ 不写业务逻辑（那是 Service 的事）</li>
 * </ul>
 *
 * <p>所以你看下面每个方法都只有两三行 —— 这是<b>正常的</b>，
 * 说明分层是清晰的。如果 Controller 里有大段逻辑，就是分层被破坏了。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "01-认证与用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     *
     * <pre>
     * POST /api/auth/login
     * { "username": "admin", "password": "admin123" }
     * </pre>
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return Result.ok("登录成功", vo);
    }

    /**
     * 用户注册
     *
     * <pre>
     * POST /api/auth/register
     * { "username": "2025210199", "password": "123456", "nickname": "王同学" }
     * </pre>
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO dto) {
        Long userId = userService.register(dto);
        return Result.ok("注册成功", userId);
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>这个接口标了 {@code @RequireLogin}，所以必须带 Token 才能访问。
     * 它是验证"拦截器是否生效"的最好例子。</p>
     */
    @ApiOperation("获取当前登录用户信息")
    @RequireLogin
    @GetMapping("/me")
    public Result<UserVO> currentUser() {
        // 直接从上下文取用户 ID，不用前端传（传了也不可信）
        Long userId = UserContext.getUserId();
        return Result.ok(userService.getUserVOById(userId));
    }

    /**
     * 退出登录
     *
     * <p><b>说明</b>：JWT 是无状态的，服务端不保存登录记录，
     * 所以严格来说不需要"退出接口"——前端把本地 Token 删掉即可。</p>
     *
     * <p>但如果要实现"强制下线"（比如管理员踢人），
     * 就需要把 Token 加入 Redis 黑名单，这个在 M3 阶段实现。</p>
     */
    @ApiOperation("退出登录")
    @RequireLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = UserContext.getUserId();
        log.info("用户退出登录 | userId={}", userId);
        // 前端负责清除本地 Token
        return Result.ok("已退出登录", null);
    }
}
