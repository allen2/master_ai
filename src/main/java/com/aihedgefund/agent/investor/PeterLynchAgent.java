package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 彼得·林奇 — 寻找十倍股，投资你了解的 */
@Component
public class PeterLynchAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/peter_lynch.md） */
    private static final String PROMPT_NAME = "peter_lynch";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public PeterLynchAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "peter_lynch"; }
    @Override public String getDisplayName() { return "彼得·林奇"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
