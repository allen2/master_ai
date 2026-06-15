package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.MessageBoardLikeMapper;
import com.aihedgefund.mapper.MessageBoardMapper;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.MessageBoardLikeDO;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.resp.MessageBoardResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 留言板业务逻辑：发表、分页查询、点赞切换、删除（本人/管理员）。
 */
@Service
public class MessageBoardService {

    private static final Logger log = LoggerFactory.getLogger(MessageBoardService.class);

    private final MessageBoardMapper messageBoardMapper;
    private final MessageBoardLikeMapper messageBoardLikeMapper;
    private final UserMapper userMapper;

    public MessageBoardService(MessageBoardMapper messageBoardMapper,
            MessageBoardLikeMapper messageBoardLikeMapper, UserMapper userMapper) {
        this.messageBoardMapper = messageBoardMapper;
        this.messageBoardLikeMapper = messageBoardLikeMapper;
        this.userMapper = userMapper;
    }

    /**
     * 发表留言。
     *
     * @param userId  发表人 id
     * @param content 留言内容
     * @return 新建留言详情
     */
    public MessageBoardResp create(Long userId, String content) {
        MessageBoardDO entity = new MessageBoardDO();
        entity.setUserId(userId);
        entity.setContent(content);
        entity.setLikeCount(0);
        entity.setDeleted(0);
        messageBoardMapper.insert(entity);
        log.info("发表留言, id={}, userId={}", entity.getId(), userId);

        return toResp(entity, userId, resolveNickname(userId), false);
    }

    /**
     * 分页查询留言列表（按 id 倒序），附带当前用户的点赞状态和删除权限。
     */
    public List<MessageBoardResp> listPage(Long currentUserId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return messageBoardMapper.selectPage(currentUserId, offset, pageSize).stream()
                .map(item -> toResp(item, currentUserId, item.getNickname(), item.getLikeRecordId() != null))
                .collect(Collectors.toList());
    }

    /** 统计未删除的留言总数 */
    public int countAll() {
        return messageBoardMapper.countAll();
    }

    /**
     * 删除自己的留言，非本人留言抛出 403。
     */
    public void deleteOwn(Long id, Long userId) {
        MessageBoardDO entity = getActiveOrThrow(id);
        if (!entity.getUserId().equals(userId)) {
            throw new BizException(403, "无权删除该留言");
        }
        messageBoardMapper.softDeleteById(id);
        log.info("删除留言, id={}, userId={}", id, userId);
    }

    /**
     * 管理员删除任意留言。
     */
    public void deleteByAdmin(Long id) {
        getActiveOrThrow(id);
        messageBoardMapper.softDeleteById(id);
        log.info("管理员删除留言, id={}", id);
    }

    /**
     * 切换点赞/取消点赞状态，返回更新后的留言详情。
     */
    @Transactional
    public MessageBoardResp toggleLike(Long id, Long userId) {
        MessageBoardDO entity = getActiveOrThrow(id);

        MessageBoardLikeDO existing = messageBoardLikeMapper.selectByMessageAndUser(id, userId);
        boolean likedByMe;
        if (existing != null) {
            messageBoardLikeMapper.deleteByMessageAndUser(id, userId);
            messageBoardMapper.updateLikeCount(id, -1);
            likedByMe = false;
            log.info("取消点赞, messageId={}, userId={}", id, userId);
        } else {
            MessageBoardLikeDO like = new MessageBoardLikeDO();
            like.setMessageId(id);
            like.setUserId(userId);
            messageBoardLikeMapper.insert(like);
            messageBoardMapper.updateLikeCount(id, 1);
            likedByMe = true;
            log.info("点赞, messageId={}, userId={}", id, userId);
        }

        MessageBoardDO updated = messageBoardMapper.selectById(id);
        return toResp(updated, userId, resolveNickname(entity.getUserId()), likedByMe);
    }

    /**
     * 查询未逻辑删除的留言，不存在或已删除抛出 404。
     */
    private MessageBoardDO getActiveOrThrow(Long id) {
        MessageBoardDO entity = messageBoardMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BizException(404, "留言不存在");
        }
        return entity;
    }

    /** 解析昵称，无昵称回退为用户名；用户不存在返回 null */
    private String resolveNickname(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return (user.getNickname() != null && !user.getNickname().isBlank())
                ? user.getNickname() : user.getUsername();
    }

    private MessageBoardResp toResp(MessageBoardDO entity, Long currentUserId, String nickname, boolean likedByMe) {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(entity.getId());
        resp.setUserId(entity.getUserId());
        resp.setNickname(nickname);
        resp.setContent(entity.getContent());
        resp.setLikeCount(entity.getLikeCount());
        resp.setLikedByMe(likedByMe);
        resp.setCanDelete(entity.getUserId().equals(currentUserId));
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
