package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardLikeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 留言点赞记录数据访问
 */
@Mapper
public interface MessageBoardLikeMapper {

    /** 查询当前用户对某条留言的点赞记录，未点赞返回 null */
    MessageBoardLikeDO selectByMessageAndUser(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /** 新增点赞记录 */
    int insert(MessageBoardLikeDO like);

    /** 删除点赞记录（取消点赞） */
    int deleteByMessageAndUser(@Param("messageId") Long messageId, @Param("userId") Long userId);
}
