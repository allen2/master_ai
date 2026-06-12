package com.aihedgefund.controller;

import com.aihedgefund.model.resp.ApiKeyResp;
import com.aihedgefund.service.ApiKeyService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiKeyService apiKeyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void list_returnsEmptyList() throws Exception {
        when(apiKeyService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api-keys"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void list_returnsApiKeys() throws Exception {
        ApiKeyResp resp = new ApiKeyResp();
        resp.setId(1L);
        resp.setProvider("OPENAI_API_KEY");
        resp.setIsActive(true);
        when(apiKeyService.listAll()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("OPENAI_API_KEY"))
                .andExpect(jsonPath("$[0].is_active").value(true));
    }

    @Test
    void saveOrUpdate_missingProvider_returns422() throws Exception {
        mockMvc.perform(post("/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyValue\":\"sk-123\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void saveOrUpdate_validRequest_returns200() throws Exception {
        ApiKeyResp resp = new ApiKeyResp();
        resp.setId(1L);
        resp.setProvider("OPENAI_API_KEY");
        resp.setIsActive(true);
        when(apiKeyService.saveOrUpdate(any())).thenReturn(resp);

        mockMvc.perform(post("/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"OPENAI_API_KEY\",\"keyValue\":\"sk-test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("OPENAI_API_KEY"));
    }
}
