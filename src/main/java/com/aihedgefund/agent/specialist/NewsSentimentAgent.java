package com.aihedgefund.agent.specialist;

import com.aihedgefund.agent.BaseLlmAgent;
import com.aihedgefund.llm.LlmClientFactory;
import com.aihedgefund.llm.StructuredOutputHelper;
import com.aihedgefund.prompt.PromptLoader;
import org.springframework.stereotype.Component;

/**
 * 新闻情绪专项 Agent
 * 替代 Python src/agents/news_sentiment.py
 */
@Component
public class NewsSentimentAgent extends BaseLlmAgent {

    /** 提示词文件名（对应 classpath:prompts/news_sentiment_analyst.md） */
    private static final String PROMPT_NAME = "news_sentiment_analyst";

    /** 系统提示词，启动时从 md 文件加载 */
    private final String systemPrompt;

    public NewsSentimentAgent(LlmClientFactory f, StructuredOutputHelper h, PromptLoader promptLoader) {
        super(f, h);
        this.systemPrompt = promptLoader.load(PROMPT_NAME);
    }

    @Override public String getAgentId() { return "news_sentiment_analyst"; }
    @Override public String getDisplayName() { return "新闻情绪分析师"; }

    @Override
    protected String getSystemPrompt() {
        return systemPrompt;
    }
}
