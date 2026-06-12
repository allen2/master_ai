package com.aihedgefund.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CachingToolCallback 测试：缓存命中/未命中 + 活动监听回调。
 */
class CachingToolCallbackTest {

    private ToolCallback delegate;
    private ToolCallCacheService cacheService;
    private List<String> activities;
    private ToolActivityListener listener;

    @BeforeEach
    void setUp() {
        delegate = Mockito.mock(ToolCallback.class);
        ToolDefinition def = Mockito.mock(ToolDefinition.class);
        when(def.name()).thenReturn("getDaily");
        when(delegate.getToolDefinition()).thenReturn(def);

        cacheService = Mockito.mock(ToolCallCacheService.class);

        activities = new ArrayList<>();
        listener = (toolName, cacheHit) -> activities.add(toolName + ":" + cacheHit);
    }

    /** 缓存命中：返回缓存、不调用 delegate，活动回调 cacheHit=true */
    @Test
    void call_cacheHit_notifiesHitAndSkipsDelegate() {
        when(cacheService.get("getDaily", "input")).thenReturn("CACHED");

        CachingToolCallback cb = new CachingToolCallback(delegate, cacheService, listener);
        String result = cb.call("input");

        assertThat(result).isEqualTo("CACHED");
        verify(delegate, never()).call(anyString());
        assertThat(activities).containsExactly("getDaily:true");
    }

    /** 缓存未命中：调用 delegate、写回缓存，活动回调 cacheHit=false */
    @Test
    void call_cacheMiss_notifiesCallAndDelegates() {
        when(cacheService.get("getDaily", "input")).thenReturn(null);
        when(delegate.call("input")).thenReturn("FRESH");

        CachingToolCallback cb = new CachingToolCallback(delegate, cacheService, listener);
        String result = cb.call("input");

        assertThat(result).isEqualTo("FRESH");
        verify(delegate).call("input");
        verify(cacheService).put("getDaily", "input", "FRESH");
        assertThat(activities).containsExactly("getDaily:false");
    }

    /** 无监听器（null）时不报错 */
    @Test
    void call_nullListener_works() {
        when(cacheService.get("getDaily", "input")).thenReturn("CACHED");

        CachingToolCallback cb = new CachingToolCallback(delegate, cacheService);
        String result = cb.call("input");

        assertThat(result).isEqualTo("CACHED");
    }
}
