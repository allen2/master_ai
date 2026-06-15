package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageBoardMapper 集成测试（内存 SQLite）。
 */
@SpringBootTest
class MessageBoardMapperTest {

    @Autowired
    private MessageBoardMapper messageBoardMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void insertAndSelectById_returnsMessageWithDefaults() {
        Long userId = createUser("mb-mapper-insert@example.com").getId();

        MessageBoardDO entity = new MessageBoardDO();
        entity.setUserId(userId);
        entity.setContent("第一条留言");
        messageBoardMapper.insert(entity);

        MessageBoardDO saved = messageBoardMapper.selectById(entity.getId());
        assertThat(saved.getContent()).isEqualTo("第一条留言");
        assertThat(saved.getLikeCount()).isEqualTo(0);
        assertThat(saved.getDeleted()).isEqualTo(0);
    }

    @Test
    void selectPage_returnsNewestFirst_withNicknameAndLikedByMe() {
        UserDO author = createUser("mb-mapper-author@example.com");
        UserDO liker = createUser("mb-mapper-liker@example.com");

        MessageBoardDO first = new MessageBoardDO();
        first.setUserId(author.getId());
        first.setContent("留言一");
        messageBoardMapper.insert(first);

        MessageBoardDO second = new MessageBoardDO();
        second.setUserId(author.getId());
        second.setContent("留言二");
        messageBoardMapper.insert(second);

        messageBoardMapper.updateLikeCount(second.getId(), 1);

        List<MessageBoardDO> page = messageBoardMapper.selectPage(liker.getId(), 0, 10);
        assertThat(page.get(0).getId()).isEqualTo(second.getId());
        assertThat(page.get(0).getNickname()).isEqualTo(author.getNickname());
        assertThat(page.get(0).getLikeCount()).isEqualTo(1);
        assertThat(page.get(0).getLikeRecordId()).isNull();
    }

    @Test
    void softDeleteById_excludesFromSelectPageAndCount() {
        Long userId = createUser("mb-mapper-delete@example.com").getId();

        MessageBoardDO entity = new MessageBoardDO();
        entity.setUserId(userId);
        entity.setContent("待删除留言");
        messageBoardMapper.insert(entity);

        int beforeTotal = messageBoardMapper.countAll();

        int affected = messageBoardMapper.softDeleteById(entity.getId());
        assertThat(affected).isEqualTo(1);

        MessageBoardDO deleted = messageBoardMapper.selectById(entity.getId());
        assertThat(deleted.getDeleted()).isEqualTo(1);
        assertThat(messageBoardMapper.countAll()).isEqualTo(beforeTotal - 1);

        List<MessageBoardDO> page = messageBoardMapper.selectPage(userId, 0, 50);
        assertThat(page).extracting(MessageBoardDO::getId).doesNotContain(entity.getId());
    }

    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setUsername(email);
        user.setPasswordHash("test-hash");
        user.setNickname("测试用户-" + email);
        userMapper.insert(user);
        return user;
    }
}
