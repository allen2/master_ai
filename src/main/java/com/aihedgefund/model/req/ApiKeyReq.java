package com.aihedgefund.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建/更新 API Key 入参
 */
public class ApiKeyReq {

    @NotBlank(message = "provider 不能为空")
    private String provider;

    @NotBlank(message = "keyValue 不能为空")
    @JsonProperty("keyValue")
    private String keyValue;

    @JsonProperty("is_active")
    private Boolean isActive = true;

    private String description;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
