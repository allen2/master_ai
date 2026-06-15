package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.MessageBoardLikeMapper;
import com.aihedgefund.mapper.MessageBoardMapper;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.MessageBoardLikeDO;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.resp.MessageBoardResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageBoardServiceTest {

    @Mock
    private MessageBoardMapper messageBoardMapper;

    @Mock
    private MessageBoardLikeMapper messageBoardLikeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MessageBoardService messageBoardService;

    @Test
    void create_returnsRespWithNicknameAndCanDeleteTrue() {
        UserDO user = new UserDO();
        user.setId(1L);
        user.setUsername("user1@example.com");
        user.setNickname("小明");
        when(userMapper.selectById(1L)).thenReturn(user);

        MessageBoardDO saved = new MessageBoardDO();
        saved.setId(10L);
        saved.setUserId(1L);
        saved.setContent("hello");
        saved.setLikeCount(0);
        saved.setDeleted(0);
        saved.setCreatedAt("2026-06-15 10:00:00");
        saved.setUpdatedAt("2026-06-15 10:00:00");
        lenient().when(messageBoardMapper.selectById(10L)).thenReturn(saved);

        MessageBoardResp resp = messageBoardService.create(1L, "hello");

        assertThat(resp.getContent()).isEqualTo("hello");
        assertThat(resp.getNickname()).isEqualTo("小明");
        assertThat(resp.getCanDelete()).isTrue();
        assertThat(resp.getLikedByMe()).isFalse();
        verify(messageBoardMapper).insert(any(MessageBoardDO.class));
    }

    @Test
    void deleteOwn_notOwner_throwsForbidden() {
        MessageBoardDO entity = new MessageBoardDO();
        entity.setId(5L);
        entity.setUserId(2L);
        entity.setDeleted(0);
        when(messageBoardMapper.selectById(5L)).thenReturn(entity);

        assertThatThrownBy(() -> messageBoardService.deleteOwn(5L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权删除");
    }

    @Test
    void deleteOwn_notFound_throws404() {
        when(messageBoardMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> messageBoardService.deleteOwn(99L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void toggleLike_notLikedYet_insertsLikeAndIncreasesCount() {
        MessageBoardDO before = new MessageBoardDO();
        before.setId(7L);
        before.setUserId(2L);
        before.setDeleted(0);
        before.setLikeCount(0);

        MessageBoardDO after = new MessageBoardDO();
        after.setId(7L);
        after.setUserId(2L);
        after.setDeleted(0);
        after.setLikeCount(1);

        when(messageBoardMapper.selectById(7L)).thenReturn(before, after);
        when(messageBoardLikeMapper.selectByMessageAndUser(7L, 1L)).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(null);

        MessageBoardResp resp = messageBoardService.toggleLike(7L, 1L);

        verify(messageBoardLikeMapper).insert(any(MessageBoardLikeDO.class));
        verify(messageBoardMapper).updateLikeCount(7L, 1);
        assertThat(resp.getLikedByMe()).isTrue();
        assertThat(resp.getLikeCount()).isEqualTo(1);
    }

    @Test
    void toggleLike_alreadyLiked_removesLikeAndDecreasesCount() {
        MessageBoardDO before = new MessageBoardDO();
        before.setId(7L);
        before.setUserId(2L);
        before.setDeleted(0);
        before.setLikeCount(1);

        MessageBoardDO after = new MessageBoardDO();
        after.setId(7L);
        after.setUserId(2L);
        after.setDeleted(0);
        after.setLikeCount(0);

        when(messageBoardMapper.selectById(7L)).thenReturn(before, after);
        MessageBoardLikeDO existing = new MessageBoardLikeDO();
        existing.setId(100L);
        existing.setMessageId(7L);
        existing.setUserId(1L);
        when(messageBoardLikeMapper.selectByMessageAndUser(7L, 1L)).thenReturn(existing);
        when(userMapper.selectById(2L)).thenReturn(null);

        MessageBoardResp resp = messageBoardService.toggleLike(7L, 1L);

        verify(messageBoardLikeMapper).deleteByMessageAndUser(7L, 1L);
        verify(messageBoardMapper).updateLikeCount(7L, -1);
        assertThat(resp.getLikedByMe()).isFalse();
        assertThat(resp.getLikeCount()).isEqualTo(0);
    }
}
