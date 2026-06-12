package com.aihedgefund.controller;

import com.aihedgefund.model.DO.FlowRunDO;
import com.aihedgefund.model.req.FlowReq;
import com.aihedgefund.model.resp.FlowResp;
import com.aihedgefund.service.FlowService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Flow 管理接口（兼容 Python /flows 和 /flow-runs 路由）
 */
@RestController
public class FlowController {

    private static final Logger log = LoggerFactory.getLogger(FlowController.class);

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @GetMapping("/flows")
    public List<FlowResp> listFlows() {
        return flowService.listAll();
    }

    @GetMapping("/flows/{id}")
    public FlowResp getFlow(@PathVariable Long id) {
        return flowService.getById(id);
    }

    @PostMapping("/flows")
    public FlowResp createFlow(@RequestBody @Valid FlowReq req) {
        return flowService.create(req);
    }

    @PutMapping("/flows/{id}")
    public FlowResp updateFlow(@PathVariable Long id, @RequestBody @Valid FlowReq req) {
        return flowService.update(id, req);
    }

    @DeleteMapping("/flows/{id}")
    public ResponseEntity<Void> deleteFlow(@PathVariable Long id) {
        flowService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/flow-runs/{id}")
    public FlowRunDO getFlowRun(@PathVariable Long id) {
        return flowService.getFlowRun(id);
    }
}
