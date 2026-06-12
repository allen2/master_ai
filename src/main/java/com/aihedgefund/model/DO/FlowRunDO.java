package com.aihedgefund.model.DO;

/**
 * hedge_fund_flow_runs 表映射
 */
public class FlowRunDO {
    private Long id;
    private Long flowId;
    private String status;
    private String tradingMode;
    private String schedule;
    private String duration;
    private String requestData;
    private String initialPortfolio;
    private String finalPortfolio;
    private String results;
    private String errorMessage;
    private Integer runNumber;
    private String startedAt;
    private String completedAt;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlowId() { return flowId; }
    public void setFlowId(Long flowId) { this.flowId = flowId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTradingMode() { return tradingMode; }
    public void setTradingMode(String tradingMode) { this.tradingMode = tradingMode; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getRequestData() { return requestData; }
    public void setRequestData(String requestData) { this.requestData = requestData; }
    public String getInitialPortfolio() { return initialPortfolio; }
    public void setInitialPortfolio(String initialPortfolio) { this.initialPortfolio = initialPortfolio; }
    public String getFinalPortfolio() { return finalPortfolio; }
    public void setFinalPortfolio(String finalPortfolio) { this.finalPortfolio = finalPortfolio; }
    public String getResults() { return results; }
    public void setResults(String results) { this.results = results; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getRunNumber() { return runNumber; }
    public void setRunNumber(Integer runNumber) { this.runNumber = runNumber; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
