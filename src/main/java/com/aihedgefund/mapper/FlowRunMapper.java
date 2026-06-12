package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.FlowRunDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * FlowRun 数据访问
 */
@Mapper
public interface FlowRunMapper {

    List<FlowRunDO> selectByFlowId(@Param("flowId") Long flowId);

    FlowRunDO selectById(@Param("id") Long id);

    int insert(FlowRunDO flowRun);

    int updateById(FlowRunDO flowRun);
}
