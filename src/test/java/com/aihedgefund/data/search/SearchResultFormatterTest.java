package com.aihedgefund.data.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchResultFormatter 单元测试。
 */
class SearchResultFormatterTest {

    @Test
    void format_withItems_buildsNumberedText() {
        List<SearchResultFormatter.Item> items = List.of(
                new SearchResultFormatter.Item("标题A", "https://a.com", "摘要A"),
                new SearchResultFormatter.Item("标题B", "https://b.com", "摘要B"));

        String text = SearchResultFormatter.format("tavily", "测试查询", "这是概要", items);

        assertThat(text).contains("[tavily 搜索: 测试查询]");
        assertThat(text).contains("概要: 这是概要");
        assertThat(text).contains("1. 标题A");
        assertThat(text).contains("https://a.com");
        assertThat(text).contains("2. 标题B");
    }

    @Test
    void format_emptyItems_showsNoResultHint() {
        String text = SearchResultFormatter.format("serper", "查询", null, List.of());

        assertThat(text).contains("（未找到相关结果）");
    }

    @Test
    void format_longSnippet_isTruncated() {
        String longSnippet = "x".repeat(800);
        List<SearchResultFormatter.Item> items = List.of(
                new SearchResultFormatter.Item("标题", "https://a.com", longSnippet));

        String text = SearchResultFormatter.format("bocha", "查询", null, items);

        assertThat(text).contains("...");
        // 截断后整体长度应远小于原始 800
        assertThat(text.length()).isLessThan(700);
    }
}
