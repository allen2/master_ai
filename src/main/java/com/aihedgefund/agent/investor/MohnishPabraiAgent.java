package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 莫尼什·帕布莱 — Dhandho 投资者，低风险高回报 */
@Component
public class MohnishPabraiAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/mohnish_pabrai.md） */
    private static final String PROMPT_NAME = "mohnish_pabrai";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public MohnishPabraiAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "mohnish_pabrai"; }
    @Override public String getDisplayName() { return "莫尼什·帕布莱"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
