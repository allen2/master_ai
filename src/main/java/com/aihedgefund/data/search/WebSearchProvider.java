package com.aihedgefund.data.search;

import reactor.core.publisher.Mono;

/**
 * 网络搜索服务商抽象。
 *
 * <p>不同服务商（Tavily / Serper / Bocha）实现本接口，统一对外提供
 * 「输入查询词，返回格式化后的搜索结果文本」的能力，便于 LLM Agent 消费。
 * 具体激活哪个服务商由配置 {@code web-search.provider} 决定。</p>
 */
public interface WebSearchProvider {

    /**
     * 服务商名称（小写），用于按配置选择激活的服务商。
     *
     * @return 服务商标识，如 tavily / serper / bocha
     */
    String name();

    /**
     * 执行网络搜索。
     *
     * @param query      查询词
     * @param maxResults 期望返回的最大结果条数
     * @return 格式化后的搜索结果文本（标题 / 链接 / 摘要）
     */
    Mono<String> search(String query, int maxResults);
}
