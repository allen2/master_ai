package com.aihedgefund.agent.specialist;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/**
 * 估值专项 Agent — 计算内在价值，生成交易信号
 * 替代 Python src/agents/valuation.py
 */
@Component
public class ValuationAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/valuation_analyst.md） */
    private static final String PROMPT_NAME = "valuation_analyst";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public ValuationAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "valuation_analyst"; }
    @Override public String getDisplayName() { return "估值分析师"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
