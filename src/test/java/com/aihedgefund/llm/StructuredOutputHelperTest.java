package com.aihedgefund.llm;

import com.aihedgefund.orchestrator.AgentSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StructuredOutputHelperTest {

    private StructuredOutputHelper helper;
    private ChatModel mockLlm;

    @BeforeEach
    void setUp() {
        helper = new StructuredOutputHelper();
        mockLlm = Mockito.mock(ChatModel.class);
    }

    private ChatResponse mockResponse(String text) {
        AssistantMessage msg = new AssistantMessage(text);
        return new ChatResponse(List.of(new Generation(msg)));
    }

    @Test
    void call_validJson_returnsSignal() {
        String json = "{\"signal\":\"bullish\",\"confidence\":75,\"reasoning\":\"strong fundamentals\"}";
        when(mockLlm.call(any(Prompt.class))).thenReturn(mockResponse(json));

        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> AgentSignal.neutral(30, "default"), 3);

        assertThat(result.getSignal()).isEqualTo("bullish");
        assertThat(result.getConfidence()).isEqualTo(75);
        assertThat(result.getReasoning()).isEqualTo("strong fundamentals");
    }

    @Test
    void call_jsonInCodeBlock_extractsCorrectly() {
        String response = "Here is the analysis:\n```json\n{\"signal\":\"bearish\",\"confidence\":60,\"reasoning\":\"high debt\"}\n```";
        when(mockLlm.call(any(Prompt.class))).thenReturn(mockResponse(response));

        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> AgentSignal.neutral(30, "default"), 3);

        assertThat(result.getSignal()).isEqualTo("bearish");
    }

    @Test
    void call_invalidJson_returnsDefault() {
        when(mockLlm.call(any(Prompt.class)))
                .thenReturn(mockResponse("Sorry, I cannot analyze this."));

        AgentSignal defaultSignal = AgentSignal.neutral(25, "fallback");
        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> defaultSignal, 1);

        assertThat(result.getSignal()).isEqualTo("neutral");
        assertThat(result.getReasoning()).isEqualTo("fallback");
    }

    @Test
    void call_embeddedJson_extractsCorrectly() {
        String response = "Analysis result: {\"signal\":\"neutral\",\"confidence\":50,\"reasoning\":\"mixed signals\"} based on the data.";
        when(mockLlm.call(any(Prompt.class))).thenReturn(mockResponse(response));

        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> AgentSignal.neutral(30, "default"), 1);

        assertThat(result.getSignal()).isEqualTo("neutral");
    }

    @Test
    void call_llmThrowsException_retriesAndReturnsDefault() {
        when(mockLlm.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        AgentSignal defaultSignal = AgentSignal.neutral(10, "error fallback");
        AgentSignal result = helper.call(mockLlm, "system", "user",
                AgentSignal.class, () -> defaultSignal, 2);

        assertThat(result.getReasoning()).isEqualTo("error fallback");
    }

    @Test
    void parse_validJson_returnsSignal() {
        String json = "{\"signal\":\"bullish\",\"confidence\":80,\"reasoning\":\"工具调用模式\"}";

        AgentSignal result = helper.parse(json, AgentSignal.class,
                () -> AgentSignal.neutral(30, "default"));

        assertThat(result.getSignal()).isEqualTo("bullish");
        assertThat(result.getConfidence()).isEqualTo(80);
    }

    @Test
    void parse_blankContent_returnsDefault() {
        AgentSignal defaultSig = AgentSignal.neutral(10, "no content");

        AgentSignal result = helper.parse("", AgentSignal.class, () -> defaultSig);

        assertThat(result.getReasoning()).isEqualTo("no content");
    }
}
