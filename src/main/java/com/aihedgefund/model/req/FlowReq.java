package com.aihedgefund.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建/更新 Flow 入参
 */
public class FlowReq {

    @NotBlank(message = "name 不能为空")
    private String name;

    private String description;
    private String nodes = "[]";
    private String edges = "[]";
    private String viewport;
    private String data;

    @JsonProperty("is_template")
    private Boolean isTemplate = false;

    private String tags;

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
    public Boolean getIsTemplate() { return isTemplate; }
    public void setIsTemplate(Boolean isTemplate) { this.isTemplate = isTemplate; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
