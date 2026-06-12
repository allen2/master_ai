package com.aihedgefund.model.DO;

/**
 * 用户分析记录实体（对应 analysis_runs 表）
 */
public class AnalysisRunDO {

    private Long id;
    private Long userId;
    private String tickers;
    private String modelName;
    /** 选中的分析师 ID 列表（JSON 数组字符串），未指定时为 null */
    private String selectedAnalysts;
    /** 状态：RUNNING / COMPLETE / ERROR */
    private String status;
    /** 分析师信号（JSON 字符串） */
    private String analystSignals;
    /** 交易决策（JSON 字符串） */
    private String decisions;
    private String errorMessage;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTickers() { return tickers; }
    public void setTickers(String tickers) { this.tickers = tickers; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getSelectedAnalysts() { return selectedAnalysts; }
    public void setSelectedAnalysts(String selectedAnalysts) { this.selectedAnalysts = selectedAnalysts; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAnalystSignals() { return analystSignals; }
    public void setAnalystSignals(String analystSignals) { this.analystSignals = analystSignals; }
    public String getDecisions() { return decisions; }
    public void setDecisions(String decisions) { this.decisions = decisions; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
