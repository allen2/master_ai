package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 凯西·伍德 — 颠覆性创新投资女王 */
@Component
public class CathieWoodAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/cathie_wood.md） */
    private static final String PROMPT_NAME = "cathie_wood";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public CathieWoodAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "cathie_wood"; }
    @Override public String getDisplayName() { return "凯西·伍德"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
