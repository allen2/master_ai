package com.aihedgefund.controller;

import com.aihedgefund.model.resp.FlowResp;
import com.aihedgefund.service.FlowService;
import com.aihedgefund.support.MockMvcAuthConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)
class FlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlowService flowService;

    @Test
    void listFlows_returnsEmptyList() throws Exception {
        when(flowService.listAll()).thenReturn(List.of());
        mockMvc.perform(get("/flows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createFlow_missingName_returns422() throws Exception {
        mockMvc.perform(post("/flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createFlow_validRequest_returns200() throws Exception {
        FlowResp resp = new FlowResp();
        resp.setId(1L);
        resp.setName("My Flow");
        resp.setNodes("[]");
        resp.setEdges("[]");
        when(flowService.create(any())).thenReturn(resp);

        mockMvc.perform(post("/flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My Flow\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Flow"));
    }

    @Test
    void getFlow_notFound_returns404() throws Exception {
        when(flowService.getById(99L))
                .thenThrow(new com.aihedgefund.common.exception.BizException(404, "Flow 不存在: 99"));
        mockMvc.perform(get("/flows/99"))
                .andExpect(status().isNotFound());
    }
}
