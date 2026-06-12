package com.aihedgefund.prompt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PromptLoader 单元测试。
 *
 * <p>内容断言使用测试专用文件 {@code prompts/test_sample.md}，
 * 与会随时调整的生产提示词文字解耦。</p>
 */
class PromptLoaderTest {

    /** 测试专用提示词文件名（src/test/resources/prompts/test_sample.md） */
    private static final String TEST_PROMPT = "test_sample";

    private PromptLoader promptLoader;

    @BeforeEach
    void setUp() {
        promptLoader = new PromptLoader();
    }

    /** 加载存在的提示词文件，返回去除首尾空白的文件内容 */
    @Test
    void load_existingPrompt_returnsTrimmedFileContent() {
        String prompt = promptLoader.load(TEST_PROMPT);

        assertThat(prompt).isEqualTo("测试提示词第一行\n测试提示词第二行");
        // trim 生效：首尾无空白
        assertThat(prompt).isEqualTo(prompt.trim());
    }

    /** 二次加载返回相同内容（命中缓存） */
    @Test
    void load_calledTwice_returnsSameContent() {
        String first = promptLoader.load(TEST_PROMPT);
        String second = promptLoader.load(TEST_PROMPT);

        assertThat(second).isEqualTo(first);
    }

    /** 加载不存在的提示词文件，抛出 IllegalStateException */
    @Test
    void load_missingPrompt_throwsException() {
        assertThatThrownBy(() -> promptLoader.load("not_exist_agent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not_exist_agent");
    }
}
