package com.aihedgefund.model.DO;

/**
 * hedge_fund_flows 表映射
 */
public class HedgeFundFlowDO {
    private Long id;
    private String name;
    private String description;
    private String nodes;
    private String edges;
    private String viewport;
    private String data;
    private Integer isTemplate;
    private String tags;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNodes() { return nodes; }
    public void setNodes(String nodes) { this.nodes = nodes; }
    public String getEdges() { return edges; }
    public void setEdges(String edges) { this.edges = edges; }
    public String getViewport() { return viewport; }
    public void setViewport(String viewport) { this.viewport = viewport; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public Integer getIsTemplate() { return isTemplate; }
    public void setIsTemplate(Integer isTemplate) { this.isTemplate = isTemplate; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
