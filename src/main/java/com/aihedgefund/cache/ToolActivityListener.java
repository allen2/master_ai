package com.aihedgefund.cache;

/**
 * 工具调用活动监听器，用于在工具被调用时向外层（如 SSE）推送简短进度。
 */
@FunctionalInterface
public interface ToolActivityListener {

    /**
     * 工具被调用时回调。
     *
     * @param toolName 工具名称
     * @param cacheHit 是否命中缓存（true=直接返回缓存，false=即将请求外部数据）
     */
    void onToolCall(String toolName, boolean cacheHit);

    /**
     * 工具调用返回结果时回调（无论命中缓存还是外部调用），用于向前端展示数据片段。
     * 默认空实现，不强制所有调用方关心结果内容。
     *
     * @param toolName 工具名称
     * @param result   工具返回的原始结果（可能为 null）
     */
    default void onToolResult(String toolName, String result) {
        // 默认不处理
    }
}
