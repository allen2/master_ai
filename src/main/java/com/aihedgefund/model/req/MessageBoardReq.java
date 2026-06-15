package com.aihedgefund.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表留言入参
 */
public class MessageBoardReq {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 500, message = "留言内容不能超过 500 字")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
