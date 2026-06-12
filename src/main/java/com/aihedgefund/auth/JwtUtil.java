package com.aihedgefund.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析登录 token。
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static final String CLAIM_USERNAME = "username";

    private final SecretKey secretKey;

    private final long expireSeconds;

    public JwtUtil(
            @Value("${jwt.secret:ai-hedge-fund-default-secret-key-please-change-in-prod-0123456789}") String secret,
            @Value("${jwt.expire-seconds:86400}") long expireSeconds) {
        // HMAC-SHA256 要求密钥至少 256 位（32 字节），不足时以右侧补齐避免启动报错
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expireSeconds = expireSeconds;
    }

    /**
     * 签发 token。
     *
     * @param userId   用户 id（作为 subject）
     * @param username 用户名（自定义 claim）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 token，返回用户身份；无效/过期返回 null。
     *
     * @param token JWT 字符串
     * @return 解析出的用户身份，失败返回 null
     */
    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get(CLAIM_USERNAME, String.class);
            return new AuthUser(userId, username);
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
