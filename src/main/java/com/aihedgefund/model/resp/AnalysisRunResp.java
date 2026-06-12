package com.aihedgefund.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 分析记录响应：列表返回不含 analystSignals/decisions 详情，详情接口返回完整内容
 */
public class AnalysisRunResp {

    private Long id;
    private String tickers;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("selected_analysts")
    private List<String> selectedAnalysts;

    private String status;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("analyst_signals")
    private Map<String, Object> analystSignals;

    private Map<String, Object> decisions;

    @JsonProperty("created_at")
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTickers() { return tickers; }
    public void setTickers(String tickers) { this.tickers = tickers; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public List<String> getSelectedAnalysts() { return selectedAnalysts; }
    public void setSelectedAnalysts(List<String> selectedAnalysts) { this.selectedAnalysts = selectedAnalysts; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Map<String, Object> getAnalystSignals() { return analystSignals; }
    public void setAnalystSignals(Map<String, Object> analystSignals) { this.analystSignals = analystSignals; }
    public Map<String, Object> getDecisions() { return decisions; }
    public void setDecisions(Map<String, Object> decisions) { this.decisions = decisions; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
