package com.aihedgefund.auth;

import com.aihedgefund.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码内存存储。
 *
 * <p>每个邮箱同一时间只保存一条记录（新发送会覆盖旧记录）。
 * 定时任务每 10 分钟清理已过期条目，防止内存泄漏。</p>
 */
@Component
public class VerificationCodeStore {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeStore.class);

    @Value("${verify-code.ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${verify-code.resend-cooldown:60}")
    private int resendCooldown;

    private final SecureRandom random = new SecureRandom();

    /** key: 邮箱地址（小写），value: 验证码条目 */
    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    /**
     * 生成并保存验证码。若距上次发送不足冷却时间，则抛出 BizException。
     *
     * @param email 目标邮箱（大小写不敏感）
     * @return 生成的 6 位验证码
     */
    public String generate(String email) {
        String key = email.toLowerCase();
        CodeEntry existing = store.get(key);
        if (existing != null && !existing.used()) {
            long cooldownMs = (long) resendCooldown * 1000;
            long sinceLastSend = System.currentTimeMillis() - existing.createdAt();
            if (sinceLastSend < cooldownMs) {
                long waitSec = (cooldownMs - sinceLastSend) / 1000 + 1;
                throw new BizException("发送过于频繁，请 " + waitSec + " 秒后重试");
            }
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        long now = System.currentTimeMillis();
        store.put(key, new CodeEntry(code, now + (long) ttlSeconds * 1000, now, false));
        log.debug("生成验证码, email={}, code={}, ttl={}s", key, code, ttlSeconds);
        return code;
    }

    /**
     * 校验验证码。校验成功后立即标记为已使用，防止重复消费。
     *
     * @param email 邮箱
     * @param code  用户输入的验证码
     * @return true 表示验证通过
     */
    public boolean verify(String email, String code) {
        String key = email.toLowerCase();
        CodeEntry entry = store.get(key);
        if (entry == null) {
            log.debug("验证码不存在, email={}", key);
            return false;
        }
        if (entry.used()) {
            log.debug("验证码已使用, email={}", key);
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            log.debug("验证码已过期, email={}", key);
            store.remove(key);
            return false;
        }
        if (!entry.code().equals(code)) {
            log.debug("验证码错误, email={}", key);
            return false;
        }
        // 标记为已使用
        store.put(key, new CodeEntry(entry.code(), entry.expireAt(), entry.createdAt(), true));
        log.debug("验证码验证通过, email={}", key);
        return true;
    }

    /** 每 10 分钟清理过期条目 */
    @Scheduled(fixedDelay = 600_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        int before = store.size();
        store.entrySet().removeIf(e -> e.getValue().expireAt() < now);
        int removed = before - store.size();
        if (removed > 0) {
            log.debug("验证码存储清理完成，删除过期条目 {} 条", removed);
        }
    }

    private record CodeEntry(String code, long expireAt, long createdAt, boolean used) {}
}
