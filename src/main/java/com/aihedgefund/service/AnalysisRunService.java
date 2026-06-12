package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.AnalysisRunMapper;
import com.aihedgefund.model.DO.AnalysisRunDO;
import com.aihedgefund.model.req.HedgeFundRunReq;
import com.aihedgefund.model.resp.AnalysisRunResp;
import com.aihedgefund.orchestrator.WorkflowResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户分析记录业务逻辑：每次 /hedge-fund/run 调用生成一条记录，
 * 完成或失败后回填分析师信号、交易决策或错误信息，供用户在「分析记录」页面查看历史。
 */
@Service
public class AnalysisRunService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRunService.class);

    private static final String STATUS_RUNNING  = "RUNNING";
    private static final String STATUS_COMPLETE = "COMPLETE";
    private static final String STATUS_ERROR    = "ERROR";

    /** 产业瓶颈分析记录在 selected_analysts 字段中的标记值，用于和个股分析记录区分 */
    private static final String RUN_TYPE_INDUSTRY_ANALYSIS = "industry_analysis";

    /** 逆向对立面分析记录在 selected_analysts 字段中的标记值，用于和其他分析记录区分 */
    private static final String RUN_TYPE_CONTRARIAN_ANALYSIS = "contrarian_analysis";

    private final AnalysisRunMapper analysisRunMapper;
    private final ObjectMapper objectMapper;

    public AnalysisRunService(AnalysisRunMapper analysisRunMapper, ObjectMapper objectMapper) {
        this.analysisRunMapper = analysisRunMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 分析开始时创建一条 RUNNING 状态的记录。
     *
     * @param userId 用户 ID
     * @param req    运行请求参数
     * @return 新建记录的 ID
     */
    public Long createRunning(Long userId, HedgeFundRunReq req) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setUserId(userId);
        run.setTickers(String.join(",", req.getTickers()));
        run.setModelName(req.getModelName());
        run.setSelectedAnalysts(toJson(req.getSelectedAnalysts()));
        run.setStatus(STATUS_RUNNING);
        analysisRunMapper.insert(run);
        log.info("创建分析记录, id={}, userId={}, tickers={}", run.getId(), userId, run.getTickers());
        return run.getId();
    }

    /**
     * 分析完成后回填分析师信号和交易决策。
     */
    public void markComplete(Long id, WorkflowResult result) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setId(id);
        run.setStatus(STATUS_COMPLETE);
        run.setAnalystSignals(toJson(result.getAnalystSignals()));
        run.setDecisions(toJson(result.getDecisions()));
        analysisRunMapper.updateById(run);
        log.info("分析记录已完成, id={}", id);
    }

    /**
     * 产业瓶颈分析开始时创建一条 RUNNING 状态的记录。
     * 通过在 selected_analysts 字段写入 {@value #RUN_TYPE_INDUSTRY_ANALYSIS} 标记，
     * 与个股分析记录区分，复用同一张表和「分析记录」页面，不新增表结构。
     *
     * @param userId    用户 ID
     * @param query     用户输入的产业分析需求（一句话描述）
     * @param modelName 使用的模型名称
     * @return 新建记录的 ID
     */
    public Long createRunningIndustryAnalysis(Long userId, String query, String modelName) {
        return createRunningResearchReport(userId, query, modelName, RUN_TYPE_INDUSTRY_ANALYSIS, "产业分析");
    }

    /**
     * 产业瓶颈分析完成后回填 Markdown 报告。
     */
    public void markCompleteIndustryAnalysis(Long id, String report) {
        markCompleteResearchReport(id, report, "产业分析");
    }

    /**
     * 逆向对立面分析开始时创建一条 RUNNING 状态的记录。
     * 通过在 selected_analysts 字段写入 {@value #RUN_TYPE_CONTRARIAN_ANALYSIS} 标记，
     * 与其他分析记录区分，复用同一张表和「分析记录」页面，不新增表结构。
     *
     * @param userId    用户 ID
     * @param query     用户输入的逆向对立面分析需求（一句话描述）
     * @param modelName 使用的模型名称
     * @return 新建记录的 ID
     */
    public Long createRunningContrarianAnalysis(Long userId, String query, String modelName) {
        return createRunningResearchReport(userId, query, modelName, RUN_TYPE_CONTRARIAN_ANALYSIS, "逆向对立面分析");
    }

    /**
     * 逆向对立面分析完成后回填 Markdown 报告。
     */
    public void markCompleteContrarianAnalysis(Long id, String report) {
        markCompleteResearchReport(id, report, "逆向对立面分析");
    }

    /**
     * 创建一条以 Markdown 报告为产出的研究类分析记录（产业分析、逆向对立面分析等共用）。
     * 通过 runType 标记写入 selected_analysts 字段，与个股分析记录区分，不新增表结构。
     */
    private Long createRunningResearchReport(Long userId, String query, String modelName,
            String runType, String logLabel) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setUserId(userId);
        run.setTickers(query);
        run.setModelName(modelName);
        run.setSelectedAnalysts(toJson(Collections.singletonList(runType)));
        run.setStatus(STATUS_RUNNING);
        analysisRunMapper.insert(run);
        log.info("创建{}记录, id={}, userId={}, query={}", logLabel, run.getId(), userId, query);
        return run.getId();
    }

    /**
     * 研究类分析记录完成后回填 Markdown 报告。
     */
    private void markCompleteResearchReport(Long id, String report, String logLabel) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setId(id);
        run.setStatus(STATUS_COMPLETE);
        run.setDecisions(toJson(Collections.singletonMap("report", report)));
        analysisRunMapper.updateById(run);
        log.info("{}记录已完成, id={}", logLabel, id);
    }

    /**
     * 分析失败后回填错误信息。
     */
    public void markError(Long id, String errorMessage) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setId(id);
        run.setStatus(STATUS_ERROR);
        run.setErrorMessage(errorMessage);
        analysisRunMapper.updateById(run);
        log.info("分析记录已标记失败, id={}, error={}", id, errorMessage);
    }

    /**
     * 分页查询当前用户的分析记录（不含信号/决策详情）。
     */
    public List<AnalysisRunResp> listByUser(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return analysisRunMapper.selectByUserId(userId, offset, pageSize).stream()
                .map(this::convertSummary)
                .collect(Collectors.toList());
    }

    public int countByUser(Long userId) {
        return analysisRunMapper.countByUserId(userId);
    }

    /**
     * 查询单条分析记录详情（含信号/决策），仅限本人查看。
     */
    public AnalysisRunResp getDetail(Long id, Long userId) {
        AnalysisRunDO run = analysisRunMapper.selectByIdAndUserId(id, userId);
        if (run == null) {
            throw new BizException(404, "分析记录不存在");
        }
        AnalysisRunResp resp = convertSummary(run);
        resp.setAnalystSignals(fromJson(run.getAnalystSignals()));
        resp.setDecisions(fromJson(run.getDecisions()));
        return resp;
    }

    private AnalysisRunResp convertSummary(AnalysisRunDO run) {
        AnalysisRunResp resp = new AnalysisRunResp();
        resp.setId(run.getId());
        resp.setTickers(run.getTickers());
        resp.setModelName(run.getModelName());
        resp.setSelectedAnalysts(fromJsonList(run.getSelectedAnalysts()));
        resp.setStatus(run.getStatus());
        resp.setErrorMessage(run.getErrorMessage());
        resp.setCreatedAt(run.getCreatedAt());
        return resp;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("序列化分析记录字段失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("反序列化分析记录字段失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            log.warn("反序列化分析记录字段失败: {}", e.getMessage());
            return null;
        }
    }
}
