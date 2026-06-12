package com.aihedgefund.agent.research;

import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.prompt.PromptLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * IndustryBottleneckResearchAgent 单元测试：验证系统提示词加载和分析流程返回大模型响应文本。
 */
class IndustryBottleneckResearchAgentTest {

    private LlmClientFactory mockFactory;
    private ChatModel mockChatModel;
    private IndustryBottleneckResearchAgent agent;

    @BeforeEach
    void setUp() {
        mockFactory = Mockito.mock(LlmClientFactory.class);
        mockChatModel = Mockito.mock(ChatModel.class);
        when(mockFactory.create(any(), any(), anyInt())).thenReturn(mockChatModel);

        // 使用真实 PromptLoader，从 classpath:prompts/industry_bottleneck_research.md 加载
        agent = new IndustryBottleneckResearchAgent(mockFactory, new PromptLoader());
    }

    /**
     * 系统提示词应来自 md 文件而非硬编码：
     * getSystemPrompt() 的返回值应与 PromptLoader 直接加载同名文件的结果一致。
     */
    @Test
    void getSystemPrompt_loadedFromMarkdownFile() {
        String expected = new PromptLoader().load("industry_bottleneck_research");

        String prompt = agent.getSystemPrompt();

        assertThat(prompt).isNotBlank();
        assertThat(prompt).isEqualTo(expected);
    }

    /**
     * analyze() 应返回大模型最终响应文本，并将分析过程中的活动消息推送给回调。
     */
    @Test
    void analyze_returnsLlmReportAndPublishesActivity() {
        String report = "# 一、逆向拆解瓶颈\n...完整五段式报告...";
        when(mockChatModel.call(any(Prompt.class))).thenReturn(mockResponse(report));

        List<String> activities = new ArrayList<>();

        String result = agent.analyze("分析AI产业链瓶颈", "gpt-4o", "OpenAI", activities::add);

        assertThat(result).isEqualTo(report);
        assertThat(activities).isNotEmpty();
    }

    private ChatResponse mockResponse(String text) {
        AssistantMessage msg = new AssistantMessage(text);
        return new ChatResponse(List.of(new Generation(msg)));
    }
}
