package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.MessageBoardLikeDO;
import com.aihedgefund.model.DO.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageBoardLikeMapper 集成测试（内存 SQLite）。
 */
@SpringBootTest
class MessageBoardLikeMapperTest {

    @Autowired
    private MessageBoardLikeMapper messageBoardLikeMapper;

    @Autowired
    private MessageBoardMapper messageBoardMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void insertSelectAndDelete_roundTrip() {
        UserDO author = createUser("mbl-mapper-author@example.com");
        UserDO liker = createUser("mbl-mapper-liker@example.com");

        MessageBoardDO message = new MessageBoardDO();
        message.setUserId(author.getId());
        message.setContent("被点赞的留言");
        messageBoardMapper.insert(message);

        assertThat(messageBoardLikeMapper.selectByMessageAndUser(message.getId(), liker.getId())).isNull();

        MessageBoardLikeDO like = new MessageBoardLikeDO();
        like.setMessageId(message.getId());
        like.setUserId(liker.getId());
        messageBoardLikeMapper.insert(like);

        MessageBoardLikeDO found = messageBoardLikeMapper.selectByMessageAndUser(message.getId(), liker.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(like.getId());

        int affected = messageBoardLikeMapper.deleteByMessageAndUser(message.getId(), liker.getId());
        assertThat(affected).isEqualTo(1);
        assertThat(messageBoardLikeMapper.selectByMessageAndUser(message.getId(), liker.getId())).isNull();
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
