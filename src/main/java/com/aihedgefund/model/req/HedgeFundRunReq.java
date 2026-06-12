package com.aihedgefund.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * 对冲基金运行请求（兼容 Python HedgeFundRequest）
 */
public class HedgeFundRunReq {

    @NotEmpty(message = "tickers 不能为空")
    private List<String> tickers;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    @JsonProperty("initial_cash")
    private Double initialCash = 100000.0;

    @JsonProperty("margin_requirement")
    private Double marginRequirement = 0.0;

    @JsonProperty("model_name")
    private String modelName = "gpt-4o";

    @JsonProperty("model_provider")
    private String modelProvider = "OpenAI";

    @JsonProperty("selected_analysts")
    private List<String> selectedAnalysts;

    @JsonProperty("graph_nodes")
    private List<Map<String, Object>> graphNodes;

    @JsonProperty("graph_edges")
    private List<Map<String, Object>> graphEdges;

    public List<String> getTickers() { return tickers; }
    public void setTickers(List<String> tickers) { this.tickers = tickers; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Double getInitialCash() { return initialCash; }
    public void setInitialCash(Double initialCash) { this.initialCash = initialCash; }
    public Double getMarginRequirement() { return marginRequirement; }
    public void setMarginRequirement(Double marginRequirement) { this.marginRequirement = marginRequirement; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public List<String> getSelectedAnalysts() { return selectedAnalysts; }
    public void setSelectedAnalysts(List<String> selectedAnalysts) { this.selectedAnalysts = selectedAnalysts; }
    public List<Map<String, Object>> getGraphNodes() { return graphNodes; }
    public void setGraphNodes(List<Map<String, Object>> graphNodes) { this.graphNodes = graphNodes; }
    public List<Map<String, Object>> getGraphEdges() { return graphEdges; }
    public void setGraphEdges(List<Map<String, Object>> graphEdges) { this.graphEdges = graphEdges; }
}
