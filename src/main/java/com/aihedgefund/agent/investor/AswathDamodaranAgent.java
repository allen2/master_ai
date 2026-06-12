package com.aihedgefund.agent.investor;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/** 阿斯沃斯·达摩达兰 — 估值教授，数字与故事兼顾 */
@Component
public class AswathDamodaranAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/aswath_damodaran.md） */
    private static final String PROMPT_NAME = "aswath_damodaran";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public AswathDamodaranAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "aswath_damodaran"; }
    @Override public String getDisplayName() { return "阿斯沃斯·达摩达兰"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
