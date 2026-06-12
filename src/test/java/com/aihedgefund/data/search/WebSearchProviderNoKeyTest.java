package com.aihedgefund.data.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 各搜索服务商在未配置 API Key 时的降级行为测试（不发起网络请求）。
 */
class WebSearchProviderNoKeyTest {

    @Test
    void tavily_noApiKey_returnsHintWithoutNetwork() {
        TavilySearchProvider provider = new TavilySearchProvider("https://api.tavily.com", "");

        String result = provider.search("query", 5).block();

        assertThat(provider.name()).isEqualTo("tavily");
        assertThat(result).contains("未配置 Tavily API Key");
    }

    @Test
    void serper_noApiKey_returnsHintWithoutNetwork() {
        SerperSearchProvider provider = new SerperSearchProvider("https://google.serper.dev", "");

        String result = provider.search("query", 5).block();

        assertThat(provider.name()).isEqualTo("serper");
        assertThat(result).contains("未配置 Serper API Key");
    }

    @Test
    void bocha_noApiKey_returnsHintWithoutNetwork() {
        BochaSearchProvider provider = new BochaSearchProvider("https://api.bochaai.com", "");

        String result = provider.search("query", 5).block();

        assertThat(provider.name()).isEqualTo("bocha");
        assertThat(result).contains("未配置 Bocha API Key");
    }
}
