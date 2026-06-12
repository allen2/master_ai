package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 沃伦·巴菲特 Agent — 寻找具有护城河的优质企业，以合理价格买入 */
@Component
public class WarrenBuffettAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/warren_buffett.md） */
    private static final String PROMPT_NAME = "warren_buffett";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public WarrenBuffettAgent(LlmClientFactory llmFactory, StructuredOutputHelper outputHelper,
            PromptLoader promptLoader) {
        super(llmFactory, outputHelper);
        // 系统启动（Bean 实例化）时从 classpath:prompts/warren_buffett.md 加载系统提示词
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "warren_buffett"; }
    @Override public String getDisplayName() { return "沃伦·巴菲特"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
