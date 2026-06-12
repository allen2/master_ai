package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 查理·芒格 — 理性思考，只买优质企业 */
@Component
public class CharlieMungerAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/charlie_munger.md） */
    private static final String PROMPT_NAME = "charlie_munger";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public CharlieMungerAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "charlie_munger"; }
    @Override public String getDisplayName() { return "查理·芒格"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
