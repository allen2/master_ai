package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 比尔·阿克曼 — 激进投资者，寻找可改变管理层的标的 */
@Component
public class BillAckmanAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/bill_ackman.md） */
    private static final String PROMPT_NAME = "bill_ackman";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public BillAckmanAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "bill_ackman"; }
    @Override public String getDisplayName() { return "比尔·阿克曼"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
