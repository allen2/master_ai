package com.aihedgefund.controller;

import com.aihedgefund.model.req.ApiKeyReq;
import com.aihedgefund.model.resp.ApiKeyResp;
import com.aihedgefund.service.ApiKeyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Key 管理接口（兼容 Python /api-keys 路由）
 */
@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyController.class);

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public List<ApiKeyResp> list() {
        return apiKeyService.listAll();
    }

    @PostMapping
    public ApiKeyResp saveOrUpdate(@RequestBody @Valid ApiKeyReq req) {
        return apiKeyService.saveOrUpdate(req);
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> delete(@PathVariable String provider) {
        apiKeyService.delete(provider);
        return ResponseEntity.noContent().build();
    }
}
