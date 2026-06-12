package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 斯坦利·德鲁肯米勒 — 宏观对冲基金传奇，寻找不对称机会 */
@Component
public class StanleyDruckenmillerAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/stanley_druckenmiller.md） */
    private static final String PROMPT_NAME = "stanley_druckenmiller";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public StanleyDruckenmillerAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "stanley_druckenmiller"; }
    @Override public String getDisplayName() { return "斯坦利·德鲁肯米勒"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
