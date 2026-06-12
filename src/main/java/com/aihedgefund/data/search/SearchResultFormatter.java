package com.aihedgefund.data.search;

import java.util.List;

/**
 * 搜索结果格式化工具：把不同服务商的结果统一成 LLM 友好的纯文本。
 */
public final class SearchResultFormatter {

    /** 单条摘要最大长度，超出截断，控制送入 LLM 的体积 */
    private static final int MAX_SNIPPET_LENGTH = 500;

    private SearchResultFormatter() {
    }

    /**
     * 单条搜索结果。
     */
    public static final class Item {

        private final String title;
        private final String url;
        private final String snippet;

        public Item(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }

    /**
     * 将搜索结果格式化为带编号的文本。
     *
     * @param provider 服务商名称
     * @param query    查询词
     * @param answer   概要回答（可为空）
     * @param items    结果列表
     * @return 格式化文本
     */
    public static String format(String provider, String query, String answer, List<Item> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(provider).append(" 搜索: ").append(query).append("]\n");

        if (answer != null && !answer.isBlank()) {
            sb.append("概要: ").append(answer.trim()).append("\n\n");
        }

        if (items == null || items.isEmpty()) {
            sb.append("（未找到相关结果）");
            return sb.toString();
        }

        int index = 1;
        for (Item item : items) {
            sb.append(index++).append(". ").append(safe(item.title)).append("\n");
            sb.append("   ").append(safe(item.url)).append("\n");
            sb.append("   ").append(truncate(safe(item.snippet))).append("\n");
        }
        return sb.toString().trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_SNIPPET_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }
}
