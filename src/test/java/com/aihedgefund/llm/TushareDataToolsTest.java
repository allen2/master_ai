package com.aihedgefund.llm;

import com.aihedgefund.cache.ToolCallCacheService;
import com.aihedgefund.data.client.TushareClient;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TushareDataTools 缓存逻辑单元测试。
 */
class TushareDataToolsTest {

    private TushareClient client;
    private ToolCallCacheService cacheService;
    private TushareDataTools tools;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(TushareClient.class);
        cacheService = Mockito.mock(ToolCallCacheService.class);
        tools = new TushareDataTools(client, cacheService);
    }

    private JSONObject apiResult(String value) {
        JSONObject json = new JSONObject();
        json.put("data", value);
        return json;
    }

    /** 缓存命中：直接返回缓存，不调用 API */
    @Test
    void getDaily_cacheHit_returnsCacheWithoutApiCall() {
        when(cacheService.get(eq("tushare.daily"), anyString())).thenReturn("CACHED_DAILY");

        String result = tools.getDaily("000001.SZ", "20240101", "20241231");

        assertThat(result).isEqualTo("CACHED_DAILY");
        verify(client, never()).api(anyString(), any());
        verify(cacheService, never()).put(anyString(), anyString(), anyString());
    }

    /** 缓存未命中：调用 API，并将非空结果写回缓存 */
    @Test
    void getDaily_cacheMiss_callsApiAndWritesCache() {
        when(cacheService.get(eq("tushare.daily"), anyString())).thenReturn(null);
        when(client.api(eq("daily"), any())).thenReturn(Mono.just(apiResult("ohlcv")));

        String result = tools.getDaily("000001.SZ", "20240101", "20241231");

        assertThat(result).contains("ohlcv");
        verify(client, times(1)).api(eq("daily"), any());
        verify(cacheService, times(1)).put(eq("tushare.daily"), anyString(), eq(result));
    }

    /** 缓存键应与参数一致（按 key 排序的确定性串） */
    @Test
    void getDaily_cacheMiss_usesCanonicalParamsKey() {
        when(cacheService.get(eq("tushare.daily"), anyString())).thenReturn(null);
        when(client.api(eq("daily"), any())).thenReturn(Mono.just(apiResult("x")));

        tools.getDaily("000001.SZ", "20240101", "20241231");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService).get(eq("tushare.daily"), keyCaptor.capture());
        assertThat(keyCaptor.getValue())
                .isEqualTo("end_date=20241231&start_date=20240101&ts_code=000001.SZ");
    }

    /** 缓存读异常应降级为未命中，仍能取数 */
    @Test
    void getDaily_cacheReadThrows_degradesToApiCall() {
        when(cacheService.get(anyString(), anyString())).thenThrow(new RuntimeException("db error"));
        when(client.api(eq("daily"), any())).thenReturn(Mono.just(apiResult("ohlcv")));

        String result = tools.getDaily("000001.SZ", "20240101", "20241231");

        assertThat(result).contains("ohlcv");
        verify(client, times(1)).api(eq("daily"), any());
    }

    /** 响应式工具 getIncome：缓存命中返回缓存，不调用 API */
    @Test
    void getIncome_cacheHit_returnsCacheWithoutApiCall() {
        when(cacheService.get(eq("tushare.income"), anyString())).thenReturn("CACHED_INCOME");

        String result = tools.getIncome("000001.SZ", "20240101", "20241231").block();

        assertThat(result).isEqualTo("CACHED_INCOME");
        verify(client, never()).api(anyString(), any());
    }

    /** 响应式工具 getIncome：未命中调用 API 并写回缓存 */
    @Test
    void getIncome_cacheMiss_callsApiAndWritesCache() {
        when(cacheService.get(eq("tushare.income"), anyString())).thenReturn(null);
        when(client.api(eq("income"), any())).thenReturn(Mono.just(apiResult("profit")));

        String result = tools.getIncome("000001.SZ", "20240101", "20241231").block();

        assertThat(result).contains("profit");
        verify(cacheService, times(1)).put(eq("tushare.income"), anyString(), eq(result));
    }

    /** getStockBasic 无参也走缓存 */
    @Test
    void getStockBasic_cacheMiss_callsApiAndWritesCache() {
        when(cacheService.get(eq("tushare.stock_basic"), anyString())).thenReturn(null);
        when(client.api(eq("stock_basic"), any())).thenReturn(Mono.just(apiResult("list")));

        String result = tools.getStockBasic();

        assertThat(result).contains("list");
        verify(cacheService, times(1)).put(eq("tushare.stock_basic"), anyString(), eq(result));
    }
}
