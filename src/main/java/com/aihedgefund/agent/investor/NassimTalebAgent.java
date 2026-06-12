package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 纳西姆·塔勒布 — 黑天鹅风险分析师，反脆弱策略 */
@Component
public class NassimTalebAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/nassim_taleb.md） */
    private static final String PROMPT_NAME = "nassim_taleb";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public NassimTalebAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "nassim_taleb"; }
    @Override public String getDisplayName() { return "纳西姆·塔勒布"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
