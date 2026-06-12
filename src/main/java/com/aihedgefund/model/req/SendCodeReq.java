package com.aihedgefund.model.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送邮箱验证码入参
 */
public class SendCodeReq {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入有效的邮箱地址")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
