package com.aihedgefund.model.DO;

/**
 * 金币流水实体（对应 coin_transactions 表）
 */
public class CoinTransactionDO {

    private Long id;
    private Long userId;
    /** 变动金额（正=发放，负=消耗） */
    private Integer amount;
    /** 类型：GRANT=管理员发放，DEDUCT=分析消耗 */
    private String type;
    private String reason;
    private Integer balanceAfter;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
