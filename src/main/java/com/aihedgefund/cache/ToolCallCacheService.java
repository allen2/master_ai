package com.aihedgefund.cache;

import com.aihedgefund.mapper.ToolCallCacheMapper;
import com.aihedgefund.model.DO.ToolCallCacheDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 工具调用结果缓存服务（基于 SQLite）
 *
 * <p>缓存策略：
 *   - Key = toolName + MD5(标准化参数 JSON)
 *   - TTL 默认 3600s（通过 hedge-fund.tool-cache-ttl 配置）
 *   - 命中返回缓存 JSON，未命中执行原调用后写回
 *   - 每次启动时自动清理过期条目
 * </p>
 */
@Service
public class ToolCallCacheService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallCacheService.class);

    /** 默认 TTL：1 小时，可通过 hedge-fund.tool-cache-ttl 覆盖 */
    private final int defaultTtlSeconds;

    private final ToolCallCacheMapper mapper;

    public ToolCallCacheService(ToolCallCacheMapper mapper,
            @Value("${hedge-fund.tool-cache-ttl:3600}") int ttlSeconds) {
        this.mapper = mapper;
        this.defaultTtlSeconds = ttlSeconds;
        int expired = mapper.deleteExpired();
        if (expired > 0) {
            log.info("启动清理过期缓存: {} 条", expired);
        }
        log.info("工具缓存初始化: TTL={}s, 当前缓存 {} 条", ttlSeconds, mapper.countAll());
    }

    /**
     * 查询缓存
     *
     * @param toolName   工具名称
     * @param paramsJson 参数 JSON 字符串（原始输入）
     * @return 缓存的结果 JSON，未命中返回 null
     */
    public String get(String toolName, String paramsJson) {
        String hash = hashParams(paramsJson);
        ToolCallCacheDO record = mapper.selectByToolAndHash(toolName, hash);
        if (record != null) {
            mapper.incrementHitCount(record.getId());
            log.info("工具缓存命中: {} hash={} (已命中 {} 次)",
                    toolName, hash.substring(0, 8), record.getHitCount() + 1);
            return record.getResultJson();
        }
        log.debug("工具缓存未命中: {} hash={}", toolName, hash.substring(0, 8));
        return null;
    }

    /**
     * 写入缓存
     *
     * @param toolName   工具名称
     * @param paramsJson 参数 JSON 字符串
     * @param resultJson 结果 JSON 字符串
     */
    public void put(String toolName, String paramsJson, String resultJson) {
        put(toolName, paramsJson, resultJson, defaultTtlSeconds);
    }

    /**
     * 写入缓存（指定 TTL）
     */
    public void put(String toolName, String paramsJson, String resultJson, int ttlSeconds) {
        String hash = hashParams(paramsJson);
        ToolCallCacheDO record = new ToolCallCacheDO();
        record.setToolName(toolName);
        record.setParamsHash(hash);
        record.setResultJson(resultJson);
        record.setTtlSeconds(ttlSeconds);
        mapper.insert(record);
        log.info("工具结果已缓存: {} hash={} ttl={}s", toolName, hash.substring(0, 8), ttlSeconds);
    }

    /**
     * 清除指定工具的缓存
     */
    public void evictByTool(String toolName) {
        // 通过过期机制间接实现；如有需要可扩展 mapper.deleteByToolName
        log.info("缓存清除请求: toolName={}（依赖 TTL 过期自动清理）", toolName);
    }

    /**
     * 获取缓存统计
     */
    public int getCacheSize() {
        return mapper.countAll();
    }

    /**
     * 计算参数 JSON 的 MD5 哈希
     * 注意：直接使用原始 JSON 字符串哈希，不做标准化
     * Spring AI 对同一请求产生的 toolInput 是确定性的
     */
    static String hashParams(String paramsJson) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(paramsJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /** 仅用于外部分层获取 TTL */
    int defaultTtlSeconds() {
        return defaultTtlSeconds;
    }
}
