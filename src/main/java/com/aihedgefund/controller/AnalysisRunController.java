package com.aihedgefund.controller;

import com.aihedgefund.auth.AuthUser;
import com.aihedgefund.auth.UserContext;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.model.resp.AnalysisRunResp;
import com.aihedgefund.service.AnalysisRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户分析记录接口（需登录）：查看本人的历史分析记录。
 */
@RestController
@RequestMapping("/analysis-runs")
public class AnalysisRunController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRunController.class);

    private final AnalysisRunService analysisRunService;

    public AnalysisRunController(AnalysisRunService analysisRunService) {
        this.analysisRunService = analysisRunService;
    }

    /**
     * 分页查询当前用户的分析记录列表。
     *
     * @param pageNum  页码（默认 1）
     * @param pageSize 每页条数（默认 10，最大 50）
     */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1")  int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (pageSize > 50) {
            pageSize = 50;
        }
        Long userId = currentUserId();
        log.debug("查询分析记录列表, userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);

        List<AnalysisRunResp> list = analysisRunService.listByUser(userId, pageNum, pageSize);
        int total = analysisRunService.countByUser(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 查询单条分析记录详情（含分析师信号和交易决策）。
     */
    @GetMapping("/{id}")
    public AnalysisRunResp detail(@PathVariable Long id) {
        Long userId = currentUserId();
        log.debug("查询分析记录详情, userId={}, id={}", userId, id);
        return analysisRunService.getDetail(id, userId);
    }

    private Long currentUserId() {
        AuthUser user = UserContext.get();
        if (user == null) {
            throw new BizException(401, "未登录");
        }
        return user.getUserId();
    }
}
