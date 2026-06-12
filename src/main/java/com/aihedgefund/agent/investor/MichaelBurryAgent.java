package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 迈克尔·伯里 — 逆向投资者，做空高估资产 */
@Component
public class MichaelBurryAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/michael_burry.md） */
    private static final String PROMPT_NAME = "michael_burry";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public MichaelBurryAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "michael_burry"; }
    @Override public String getDisplayName() { return "迈克尔·伯里"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
