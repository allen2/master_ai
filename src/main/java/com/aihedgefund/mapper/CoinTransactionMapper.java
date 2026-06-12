package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.CoinTransactionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 金币流水数据访问
 */
@Mapper
public interface CoinTransactionMapper {

    void insert(CoinTransactionDO tx);

    /**
     * 分页查询指定用户的金币流水，按时间倒序。
     */
    List<CoinTransactionDO> selectByUserId(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByUserId(@Param("userId") Long userId);
}
