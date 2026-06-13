package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserMapper 集成测试（内存 SQLite）。
 */
@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    /** updatePassword 应更新 password_hash 并保留其他字段不变 */
    @Test
    void updatePassword_updatesPasswordHash() {
        UserDO user = new UserDO();
        user.setUsername("update-pwd-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("old-hash");
        user.setNickname("测试用户");
        userMapper.insert(user);

        int affected = userMapper.updatePassword(user.getId(), "new-hash");
        assertThat(affected).isEqualTo(1);

        UserDO updated = userMapper.selectById(user.getId());
        assertThat(updated.getPasswordHash()).isEqualTo("new-hash");
        assertThat(updated.getUsername()).isEqualTo(user.getUsername());
    }
}
