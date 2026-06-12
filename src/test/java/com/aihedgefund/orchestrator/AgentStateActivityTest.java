package com.aihedgefund.orchestrator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * AgentState 活动进度监听器测试。
 */
class AgentStateActivityTest {

    @Test
    void publishActivity_withListener_deliversMessage() {
        AgentState state = new AgentState();
        List<String> received = new ArrayList<>();
        state.setActivityListener((agentId, message) -> received.add(agentId + "|" + message));

        state.publishActivity("warren_buffett", "调用工具 getDaily 获取数据…");

        assertThat(received).containsExactly("warren_buffett|调用工具 getDaily 获取数据…");
    }

    @Test
    void publishActivity_withoutListener_doesNotThrow() {
        AgentState state = new AgentState();

        assertThatCode(() -> state.publishActivity("a", "msg")).doesNotThrowAnyException();
    }

    @Test
    void publishActivity_listenerThrows_isSwallowed() {
        AgentState state = new AgentState();
        state.setActivityListener((agentId, message) -> {
            throw new RuntimeException("boom");
        });

        // 监听器抛错不应影响主流程
        assertThatCode(() -> state.publishActivity("a", "msg")).doesNotThrowAnyException();
    }
}
