package com.aihedgefund.controller;

import com.aihedgefund.model.resp.MessageBoardResp;
import com.aihedgefund.service.MessageBoardService;
import com.aihedgefund.support.MockMvcAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MessageBoardController 接口测试：MockMvcAuthConfig 默认以 userId=1 的身份发起请求。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)
class MessageBoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageBoardService messageBoardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void list_returnsPagedMessages() throws Exception {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(1L);
        resp.setUserId(1L);
        resp.setNickname("小明");
        resp.setContent("第一条留言");
        resp.setLikeCount(2);
        resp.setLikedByMe(true);
        resp.setCanDelete(true);
        when(messageBoardService.listPage(eq(1L), eq(1), eq(20))).thenReturn(List.of(resp));
        when(messageBoardService.countAll()).thenReturn(1);

        mockMvc.perform(get("/message-board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list[0].content").value("第一条留言"))
                .andExpect(jsonPath("$.list[0].like_count").value(2))
                .andExpect(jsonPath("$.list[0].liked_by_me").value(true))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void create_blankContent_returns422() throws Exception {
        mockMvc.perform(post("/message-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_validContent_returns200() throws Exception {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(1L);
        resp.setUserId(1L);
        resp.setContent("新留言");
        resp.setLikeCount(0);
        resp.setLikedByMe(false);
        resp.setCanDelete(true);
        when(messageBoardService.create(eq(1L), eq("新留言"))).thenReturn(resp);

        mockMvc.perform(post("/message-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"新留言\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("新留言"));
    }

    @Test
    void delete_ownMessage_returns204() throws Exception {
        mockMvc.perform(delete("/message-board/1"))
                .andExpect(status().isNoContent());

        verify(messageBoardService).deleteOwn(1L, 1L);
    }

    @Test
    void like_togglesAndReturnsUpdatedCount() throws Exception {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(1L);
        resp.setLikeCount(1);
        resp.setLikedByMe(true);
        when(messageBoardService.toggleLike(1L, 1L)).thenReturn(resp);

        mockMvc.perform(post("/message-board/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.like_count").value(1))
                .andExpect(jsonPath("$.liked_by_me").value(true));
    }
}
