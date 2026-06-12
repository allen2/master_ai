package com.aihedgefund.model.resp;

/**
 * 登录/注册成功返回：JWT token + 用户信息
 */
public class AuthResp {

    private String token;
    private Long userId;
    private String username;
    private String nickname;

    public AuthResp() {
    }

    public AuthResp(String token, Long userId, String username, String nickname) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
