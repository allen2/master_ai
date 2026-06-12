package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.ToolCallCacheDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ToolCallCacheMapper 集成测试（内存 SQLite）。
 *
 * <p>重点验证 {@code INSERT OR REPLACE}：相同 (tool_name, params_hash)
 * 重复写入不再触发唯一索引冲突，且整行被新值覆盖。</p>
 */
@SpringBootTest
class ToolCallCacheMapperTest {

    @Autowired
    private ToolCallCacheMapper mapper;

    private ToolCallCacheDO record(String toolName, String hash, String result) {
        ToolCallCacheDO record = new ToolCallCacheDO();
        record.setToolName(toolName);
        record.setParamsHash(hash);
        record.setResultJson(result);
        record.setTtlSeconds(3600);
        return record;
    }

    /** 相同键重复 insert 不抛唯一索引冲突，且新值覆盖旧值 */
    @Test
    void insert_sameKeyTwice_replacesWithoutConflict() {
        String toolName = "tushare.daily";
        String hash = UUID.randomUUID().toString().replace("-", "");

        mapper.insert(record(toolName, hash, "OLD_RESULT"));

        // 第二次写入相同 (tool_name, params_hash) —— 修复前会因唯一索引冲突抛异常
        assertThatCode(() -> mapper.insert(record(toolName, hash, "NEW_RESULT")))
                .doesNotThrowAnyException();

        ToolCallCacheDO found = mapper.selectByToolAndHash(toolName, hash);
        assertThat(found).isNotNull();
        assertThat(found.getResultJson()).isEqualTo("NEW_RESULT");
    }

    /** 不同键各自独立存储 */
    @Test
    void insert_differentKeys_storedIndependently() {
        String toolName = "tushare.income";
        String hashA = UUID.randomUUID().toString().replace("-", "");
        String hashB = UUID.randomUUID().toString().replace("-", "");

        mapper.insert(record(toolName, hashA, "RESULT_A"));
        mapper.insert(record(toolName, hashB, "RESULT_B"));

        assertThat(mapper.selectByToolAndHash(toolName, hashA).getResultJson()).isEqualTo("RESULT_A");
        assertThat(mapper.selectByToolAndHash(toolName, hashB).getResultJson()).isEqualTo("RESULT_B");
    }
}
