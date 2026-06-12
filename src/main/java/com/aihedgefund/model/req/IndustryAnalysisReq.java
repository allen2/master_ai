package com.aihedgefund.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 产业瓶颈分析请求：用户输入一句话描述需要分析的行业/标的及附加筛选条件。
 */
public class IndustryAnalysisReq {

    @NotBlank(message = "query 不能为空")
    private String query;

    @JsonProperty("model_name")
    private String modelName = "astron-code-latest";

    @JsonProperty("model_provider")
    private String modelProvider = "OpenAI";

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
}
