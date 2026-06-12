package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 菲利普·费雪 — 深度研究成长型企业 */
@Component
public class PhilFisherAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/phil_fisher.md） */
    private static final String PROMPT_NAME = "phil_fisher";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public PhilFisherAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "phil_fisher"; }
    @Override public String getDisplayName() { return "菲利普·费雪"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
