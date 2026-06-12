package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.AnalysisRunDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户分析记录数据访问
 */
@Mapper
public interface AnalysisRunMapper {

    int insert(AnalysisRunDO run);

    int updateById(AnalysisRunDO run);

    AnalysisRunDO selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 分页查询指定用户的分析记录，按时间倒序。
     */
    List<AnalysisRunDO> selectByUserId(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByUserId(@Param("userId") Long userId);
}
