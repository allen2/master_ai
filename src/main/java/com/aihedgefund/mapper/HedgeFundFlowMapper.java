package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.HedgeFundFlowDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Flow 数据访问
 */
@Mapper
public interface HedgeFundFlowMapper {

    List<HedgeFundFlowDO> selectAll();

    HedgeFundFlowDO selectById(@Param("id") Long id);

    int insert(HedgeFundFlowDO flow);

    int updateById(HedgeFundFlowDO flow);

    int deleteById(@Param("id") Long id);
}
