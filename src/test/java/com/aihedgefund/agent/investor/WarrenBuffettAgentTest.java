package com.aihedgefund.agent.investor;

import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WarrenBuffettAgent 单元测试：验证系统提示词从 md 文件加载（不耦合具体文案）。
 */
class WarrenBuffettAgentTest {

    private WarrenBuffettAgent agent;

    @BeforeEach
    void setUp() {
        LlmClientFactory mockFactory = Mockito.mock(LlmClientFactory.class);
        StructuredOutputHelper mockHelper = Mockito.mock(StructuredOutputHelper.class);
        // 使用真实 PromptLoader，从 classpath:prompts/warren_buffett.md 加载
        agent = new WarrenBuffettAgent(mockFactory, mockHelper, new PromptLoader());
    }

    @Test
    void getAgentId_returnsCorrectId() {
        assertThat(agent.getAgentId()).isEqualTo("warren_buffett");
    }

    /**
     * 系统提示词应来自 md 文件而非硬编码：
     * getSystemPrompt() 的返回值应与 PromptLoader 直接加载同名文件的结果一致。
     */
    @Test
    void getSystemPrompt_loadedFromMarkdownFile() {
        String expected = new PromptLoader().load("warren_buffett");

        String prompt = agent.getSystemPrompt();

        assertThat(prompt).isNotBlank();
        assertThat(prompt).isEqualTo(expected);
    }
}
