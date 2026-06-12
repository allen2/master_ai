package com.aihedgefund.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 可用 LLM 模型列表（兼容 Python GET /language-models）
 */
@RestController
public class ModelController {

    private static final List<Map<String, String>> MODELS = List.of(
            Map.of("display_name", "astron-code-latest", "model_name", "astron-code-latest", "provider", "OpenAI")
    );

    @GetMapping("/language-models")
    public List<Map<String, String>> listModels() {
        return MODELS;
    }
}
