package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.ApiKeyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * API Key 数据访问
 */
@Mapper
public interface ApiKeyMapper {

    List<ApiKeyDO> selectAll();

    ApiKeyDO selectByProvider(@Param("provider") String provider);

    int insert(ApiKeyDO apiKey);

    int updateByProvider(ApiKeyDO apiKey);

    int deleteByProvider(@Param("provider") String provider);
}
