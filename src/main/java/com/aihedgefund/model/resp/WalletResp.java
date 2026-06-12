package com.aihedgefund.model.resp;

/**
 * 钱包余额响应
 */
public class WalletResp {

    private Long userId;
    private Integer balance;

    public WalletResp() {}

    public WalletResp(Long userId, Integer balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getBalance() { return balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
}
