package com.nnuist.coursehub.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 —— 负责"发凭证"和"验凭证"
 *
 * <p><b>JWT 是什么</b>：一段形如 {@code xxx.yyy.zzz} 的字符串，由三部分组成：</p>
 * <pre>
 *   Header（头部）     .  Payload（载荷）        .  Signature（签名）
 *   {"alg":"HS256"}      {"userId":1,...}         用密钥算出的防伪标记
 * </pre>
 *
 * <p><b>为什么安全</b>：签名是用只有服务器知道的密钥算出来的。
 * 别人就算篡改了 Payload 里的 userId，签名也会对不上，服务端会拒绝。</p>
 *
 * <p><b>注意</b>：JWT 里的内容是 Base64 编码的，不是加密的，<b>谁都能解开看</b>。
 * 所以里面<b>绝对不能放密码</b>这种敏感信息。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Component
public class JwtUtil {

    /** 请求头里放 Token 的字段名 */
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";

    /** 密钥，从配置文件读取（生产环境必须用环境变量注入） */
    @Value("${coursehub.jwt.secret}")
    private String secret;

    /** 有效期（毫秒），默认 7 天 */
    @Value("${coursehub.jwt.expire:604800000}")
    private Long expire;

    /**
     * 获取签名密钥
     *
     * <p>HS256 算法要求密钥长度至少 256 位（32 字节），太短会抛异常。</p>
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLE, role);

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expire);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 Token，拿到里面的信息
     *
     * @return 解析结果；Token 无效或过期时返回 null
     */
    public Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token 无效: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 从 Token 中取出用户 ID
     *
     * @return 用户 ID；Token 无效时返回 null
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get(CLAIM_USER_ID);
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    /**
     * 从 Token 中取出角色
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : (String) claims.get(CLAIM_ROLE);
    }

    /**
     * 从 Token 中取出用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : (String) claims.get(CLAIM_USERNAME);
    }

    /**
     * 判断 Token 是否已过期
     */
    public boolean isExpired(String token) {
        Claims claims = parseToken(token);
        return claims == null || claims.getExpiration().before(new Date());
    }

    /**
     * 获取配置的有效期（秒），供前端展示"登录状态还剩多久"
     */
    public Long getExpireSeconds() {
        return expire / 1000;
    }
}
