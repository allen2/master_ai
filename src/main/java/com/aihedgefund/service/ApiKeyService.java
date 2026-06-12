package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.ApiKeyMapper;
import com.aihedgefund.model.DO.ApiKeyDO;
import com.aihedgefund.model.req.ApiKeyReq;
import com.aihedgefund.model.resp.ApiKeyResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API Key 业务逻辑
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyService(ApiKeyMapper apiKeyMapper) {
        this.apiKeyMapper = apiKeyMapper;
    }

    public List<ApiKeyResp> listAll() {
        log.debug("查询所有 API Key");
        return apiKeyMapper.selectAll().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    public ApiKeyResp saveOrUpdate(ApiKeyReq req) {
        log.info("保存 API Key, provider={}", req.getProvider());
        ApiKeyDO existing = apiKeyMapper.selectByProvider(req.getProvider());
        ApiKeyDO entity = new ApiKeyDO();
        entity.setProvider(req.getProvider());
        entity.setKeyValue(req.getKeyValue());
        entity.setIsActive(Boolean.TRUE.equals(req.getIsActive()) ? 1 : 0);
        entity.setDescription(req.getDescription());

        if (existing == null) {
            apiKeyMapper.insert(entity);
        } else {
            apiKeyMapper.updateByProvider(entity);
        }

        return toResp(apiKeyMapper.selectByProvider(req.getProvider()));
    }

    public void delete(String provider) {
        log.info("删除 API Key, provider={}", provider);
        if (apiKeyMapper.selectByProvider(provider) == null) {
            throw new BizException(404, "API Key 不存在: " + provider);
        }
        apiKeyMapper.deleteByProvider(provider);
    }

    /**
     * 获取指定 provider 的 key value（内部使用，不对外暴露）
     */
    public String getKeyValue(String provider) {
        ApiKeyDO entity = apiKeyMapper.selectByProvider(provider);
        return entity != null ? entity.getKeyValue() : null;
    }

    private ApiKeyResp toResp(ApiKeyDO entity) {
        ApiKeyResp resp = new ApiKeyResp();
        resp.setId(entity.getId());
        resp.setProvider(entity.getProvider());
        resp.setIsActive(entity.getIsActive() != null && entity.getIsActive() == 1);
        resp.setDescription(entity.getDescription());
        resp.setLastUsed(entity.getLastUsed());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
