package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.ToolCallCacheDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工具调用缓存数据访问
 */
@Mapper
public interface ToolCallCacheMapper {

    ToolCallCacheDO selectByToolAndHash(@Param("toolName") String toolName,
                                        @Param("paramsHash") String paramsHash);

    int insert(ToolCallCacheDO record);

    int incrementHitCount(@Param("id") Long id);

    /** 清理过期缓存（created_at + ttl_seconds < now） */
    int deleteExpired();

    /** 统计缓存条目数 */
    int countAll();

    /** 分页查询（管理后台用） */
    List<ToolCallCacheDO> selectAll(@Param("offset") int offset, @Param("limit") int limit);
}
