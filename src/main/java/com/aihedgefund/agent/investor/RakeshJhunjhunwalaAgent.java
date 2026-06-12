package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 拉克什·贾加纳坦·胡杰 — 印度股市大牛，宏观与成长双轮驱动 */
@Component
public class RakeshJhunjhunwalaAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/rakesh_jhunjhunwala.md） */
    private static final String PROMPT_NAME = "rakesh_jhunjhunwala";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public RakeshJhunjhunwalaAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "rakesh_jhunjhunwala"; }
    @Override public String getDisplayName() { return "拉克什·胡杰"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
