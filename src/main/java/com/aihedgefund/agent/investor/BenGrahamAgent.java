package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 本杰明·格雷厄姆 — 价值投资之父，寻找安全边际 */
@Component
public class BenGrahamAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/ben_graham.md） */
    private static final String PROMPT_NAME = "ben_graham";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public BenGrahamAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "ben_graham"; }
    @Override public String getDisplayName() { return "本杰明·格雷厄姆"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
