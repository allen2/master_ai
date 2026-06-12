package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.UserWalletDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户钱包数据访问
 */
@Mapper
public interface UserWalletMapper {

    UserWalletDO selectByUserId(@Param("userId") Long userId);

    void insert(UserWalletDO wallet);

    /**
     * 原子扣减 1 枚金币。余额不足时不更新（返回 0）。
     *
     * @return 影响行数：1=成功，0=余额不足
     */
    int deductOne(@Param("userId") Long userId);

    /**
     * 增加金币余额（管理员发放）。
     */
    void addBalance(@Param("userId") Long userId, @Param("amount") int amount);
}
