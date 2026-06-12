package com.aihedgefund.llm;

import com.aihedgefund.cache.ToolCallCacheService;
import com.aihedgefund.data.search.WebSearchProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSearchTools 单元测试：服务商选择 + 缓存 + 参数规范化。
 */
class WebSearchToolsTest {

    private WebSearchProvider tavily;
    private WebSearchProvider serper;
    private ToolCallCacheService cacheService;

    @BeforeEach
    void setUp() {
        tavily = Mockito.mock(WebSearchProvider.class);
        when(tavily.name()).thenReturn("tavily");
        serper = Mockito.mock(WebSearchProvider.class);
        when(serper.name()).thenReturn("serper");
        cacheService = Mockito.mock(ToolCallCacheService.class);
    }

    private WebSearchTools build(String providerName) {
        return new WebSearchTools(List.of(tavily, serper), cacheService, providerName);
    }

    /** 按配置选择激活的服务商 */
    @Test
    void constructor_selectsConfiguredProvider() {
        when(cacheService.get(eq("websearch.serper"), anyString())).thenReturn(null);
        when(serper.search(anyString(), anyInt())).thenReturn(Mono.just("SERPER_RESULT"));

        WebSearchTools tools = build("serper");
        String result = tools.webSearch("茅台 业绩", 5);

        assertThat(result).isEqualTo("SERPER_RESULT");
        verify(serper, times(1)).search(eq("茅台 业绩"), eq(5));
        verify(tavily, never()).search(anyString(), anyInt());
    }

    /** 大小写不敏感选择服务商 */
    @Test
    void constructor_providerNameCaseInsensitive() {
        WebSearchTools tools = build("TAVILY");
        when(cacheService.get(anyString(), anyString())).thenReturn(null);
        when(tavily.search(anyString(), anyInt())).thenReturn(Mono.just("R"));

        tools.webSearch("q", 3);

        verify(tavily, times(1)).search(eq("q"), eq(3));
    }

    /** 未知服务商名应抛异常 */
    @Test
    void constructor_unknownProvider_throws() {
        assertThatThrownBy(() -> build("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
    }

    /** 缓存命中：直接返回，不调用搜索 */
    @Test
    void webSearch_cacheHit_returnsCacheWithoutSearch() {
        when(cacheService.get(eq("websearch.tavily"), anyString())).thenReturn("CACHED");

        WebSearchTools tools = build("tavily");
        String result = tools.webSearch("query", 5);

        assertThat(result).isEqualTo("CACHED");
        verify(tavily, never()).search(anyString(), anyInt());
        verify(cacheService, never()).put(anyString(), anyString(), anyString());
    }

    /** 缓存未命中：调用搜索并写回缓存 */
    @Test
    void webSearch_cacheMiss_searchesAndWritesCache() {
        when(cacheService.get(eq("websearch.tavily"), anyString())).thenReturn(null);
        when(tavily.search(eq("query"), eq(5))).thenReturn(Mono.just("FRESH_RESULT"));

        WebSearchTools tools = build("tavily");
        String result = tools.webSearch("query", 5);

        assertThat(result).isEqualTo("FRESH_RESULT");
        verify(cacheService, times(1)).put(eq("websearch.tavily"), anyString(), eq("FRESH_RESULT"));
    }

    /** maxResults 为空时使用默认值 5 */
    @Test
    void webSearch_nullMaxResults_usesDefault() {
        when(cacheService.get(anyString(), anyString())).thenReturn(null);
        when(tavily.search(anyString(), anyInt())).thenReturn(Mono.just("R"));

        WebSearchTools tools = build("tavily");
        tools.webSearch("query", null);

        verify(tavily, times(1)).search(eq("query"), eq(5));
    }

    /** maxResults 超过上限时取上限 10 */
    @Test
    void webSearch_maxResultsOverLimit_cappedAtTen() {
        when(cacheService.get(anyString(), anyString())).thenReturn(null);
        when(tavily.search(anyString(), anyInt())).thenReturn(Mono.just("R"));

        WebSearchTools tools = build("tavily");
        tools.webSearch("query", 99);

        verify(tavily, times(1)).search(eq("query"), eq(10));
    }
}
