package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 留言板数据访问
 */
@Mapper
public interface MessageBoardMapper {

    /** 新增留言 */
    int insert(MessageBoardDO message);

    /** 按 id 查询留言（不含 nickname/likeRecordId） */
    MessageBoardDO selectById(@Param("id") Long id);

    /**
     * 分页查询未删除的留言，按 id 倒序，关联查询作者昵称和当前用户的点赞记录。
     *
     * @param currentUserId 当前登录用户 id，用于判断 likedByMe
     * @param offset        偏移量
     * @param limit         每页条数
     */
    List<MessageBoardDO> selectPage(@Param("currentUserId") Long currentUserId,
            @Param("offset") int offset, @Param("limit") int limit);

    /** 统计未删除的留言总数 */
    int countAll();

    /** 逻辑删除留言（仅 deleted=0 时生效），同时更新 updated_at */
    int softDeleteById(@Param("id") Long id);

    /** 原子更新点赞数（delta 为 +1 或 -1），同时更新 updated_at */
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
