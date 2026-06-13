package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问
 */
@Mapper
public interface UserMapper {

    /** 按用户名查询（仅未删除） */
    UserDO selectByUsername(@Param("username") String username);

    /** 按 id 查询（仅未删除） */
    UserDO selectById(@Param("id") Long id);

    /** 新增用户 */
    int insert(UserDO user);

    /** 更新密码（同时更新 updated_at） */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 查询所有未删除的用户（管理员用） */
    List<UserDO> selectAll();
}
