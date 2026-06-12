package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.FlowRunMapper;
import com.aihedgefund.mapper.HedgeFundFlowMapper;
import com.aihedgefund.model.DO.FlowRunDO;
import com.aihedgefund.model.DO.HedgeFundFlowDO;
import com.aihedgefund.model.req.FlowReq;
import com.aihedgefund.model.resp.FlowResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Flow 业务逻辑
 */
@Service
public class FlowService {

    private static final Logger log = LoggerFactory.getLogger(FlowService.class);

    private final HedgeFundFlowMapper flowMapper;
    private final FlowRunMapper flowRunMapper;

    public FlowService(HedgeFundFlowMapper flowMapper, FlowRunMapper flowRunMapper) {
        this.flowMapper = flowMapper;
        this.flowRunMapper = flowRunMapper;
    }

    public List<FlowResp> listAll() {
        log.debug("查询所有 Flow");
        return flowMapper.selectAll().stream().map(this::toResp).collect(Collectors.toList());
    }

    public FlowResp getById(Long id) {
        HedgeFundFlowDO entity = flowMapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "Flow 不存在: " + id);
        }
        return toResp(entity);
    }

    public FlowResp create(FlowReq req) {
        log.info("创建 Flow, name={}", req.getName());
        HedgeFundFlowDO entity = toEntity(req);
        flowMapper.insert(entity);
        return toResp(flowMapper.selectById(entity.getId()));
    }

    public FlowResp update(Long id, FlowReq req) {
        log.info("更新 Flow, id={}", id);
        if (flowMapper.selectById(id) == null) {
            throw new BizException(404, "Flow 不存在: " + id);
        }
        HedgeFundFlowDO entity = toEntity(req);
        entity.setId(id);
        flowMapper.updateById(entity);
        return toResp(flowMapper.selectById(id));
    }

    public void delete(Long id) {
        log.info("删除 Flow, id={}", id);
        if (flowMapper.selectById(id) == null) {
            throw new BizException(404, "Flow 不存在: " + id);
        }
        flowMapper.deleteById(id);
    }

    public FlowRunDO getFlowRun(Long runId) {
        FlowRunDO run = flowRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(404, "FlowRun 不存在: " + runId);
        }
        return run;
    }

    private HedgeFundFlowDO toEntity(FlowReq req) {
        HedgeFundFlowDO entity = new HedgeFundFlowDO();
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setNodes(req.getNodes() != null ? req.getNodes() : "[]");
        entity.setEdges(req.getEdges() != null ? req.getEdges() : "[]");
        entity.setViewport(req.getViewport());
        entity.setData(req.getData());
        entity.setIsTemplate(Boolean.TRUE.equals(req.getIsTemplate()) ? 1 : 0);
        entity.setTags(req.getTags());
        return entity;
    }

    private FlowResp toResp(HedgeFundFlowDO entity) {
        FlowResp resp = new FlowResp();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setNodes(entity.getNodes());
        resp.setEdges(entity.getEdges());
        resp.setViewport(entity.getViewport());
        resp.setData(entity.getData());
        resp.setIsTemplate(entity.getIsTemplate() != null && entity.getIsTemplate() == 1);
        resp.setTags(entity.getTags());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
