package com.aihedgefund.config;

import com.aihedgefund.llm.FinancialDataTools;
import com.aihedgefund.llm.TushareDataTools;
import com.aihedgefund.llm.WebSearchTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 配置
 * <p>
 * 将 FinancialDataTools 中所有带 @Tool 注解的方法注册为 MCP 工具，
 * 供外部 MCP 客户端（Claude Desktop、Cursor 等）通过 SSE 连接使用。
 * </p>
 */
@Configuration
public class McpConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpConfiguration.class);

    /**
     * 注册 FinancialDataTools 为 MCP 工具提供者。
     *
     * @param financialDataTools 金融数据工具组件
     * @return ToolCallbackProvider，包含所有 @Tool 方法
     */
//    @Bean
//    public ToolCallbackProvider financialDataToolProvider(FinancialDataTools financialDataTools) {
//        log.info("注册 FinancialDataTools 为 MCP ToolCallbackProvider");
//        return MethodToolCallbackProvider.builder()
//                .toolObjects(financialDataTools)
//                .build();
//    }

    /**
     * 注册 TushareDataTools 为 MCP 工具提供者。
     *
     * @param tushareDataTools 金融数据工具组件
     * @return ToolCallbackProvider，包含所有 @Tool 方法
     */
    @Bean
    public ToolCallbackProvider tushareDataToolProvider(TushareDataTools tushareDataTools) {
        log.info("注册 tushareDataToolProvider 为 MCP ToolCallbackProvider");
        return MethodToolCallbackProvider.builder()
                .toolObjects(tushareDataTools)
                .build();
    }

    /**
     * 注册 WebSearchTools 为 MCP 工具提供者，让 Agent 具备联网搜索能力。
     *
     * @param webSearchTools 网络搜索工具组件
     * @return ToolCallbackProvider，包含所有 @Tool 方法
     */
    @Bean
    public ToolCallbackProvider webSearchToolProvider(WebSearchTools webSearchTools) {
        log.info("注册 webSearchToolProvider 为 MCP ToolCallbackProvider");
        return MethodToolCallbackProvider.builder()
                .toolObjects(webSearchTools)
                .build();
    }
}
