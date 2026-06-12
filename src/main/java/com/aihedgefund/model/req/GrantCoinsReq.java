package com.aihedgefund.model.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 管理员发放金币入参
 *
 * <p>用户定位方式二选一：传 {@code userId} 按用户 ID 发放，或传 {@code email}
 * 按注册邮箱（即用户名）发放，两者都不传或用户不存在时返回错误。</p>
 */
public class GrantCoinsReq {

    private Long userId;

    /** 用户注册邮箱（即用户名），与 userId 二选一 */
    private String email;

    @NotNull(message = "发放数量不能为空")
    @Min(value = 1, message = "发放数量最少为 1")
    private Integer amount;

    private String reason;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
