package com.aihedgefund.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * Flow 返回（nodes/edges 作为原始 JSON 输出，不二次序列化）
 */
public class FlowResp {
    private Long id;
    private String name;
    private String description;
    private String nodes;
    private String edges;
    private String viewport;
    private String data;
    private Boolean isTemplate;
    private String tags;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @JsonRawValue
    public String getNodes() { return nodes; }
    public void setNodes(String nodes) { this.nodes = nodes; }

    @JsonRawValue
    public String getEdges() { return edges; }
    public void setEdges(String edges) { this.edges = edges; }

    @JsonRawValue
    public String getViewport() { return viewport; }
    public void setViewport(String viewport) { this.viewport = viewport; }

    @JsonRawValue
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    @JsonProperty("is_template")
    public Boolean getIsTemplate() { return isTemplate; }
    public void setIsTemplate(Boolean isTemplate) { this.isTemplate = isTemplate; }

    @JsonRawValue
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    @JsonProperty("created_at")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @JsonProperty("updated_at")
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
