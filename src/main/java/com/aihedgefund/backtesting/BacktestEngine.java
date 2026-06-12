package com.aihedgefund.backtesting;

import com.aihedgefund.data.client.FinancialDatasetsClient;
import com.aihedgefund.model.req.HedgeFundRunReq;
import com.aihedgefund.orchestrator.HedgeFundOrchestrator;
import com.aihedgefund.orchestrator.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 回测引擎（替代 Python src/backtesting/engine.py BacktestEngine）
 *
 * 逻辑：
 *   1. 按月生成回测日期序列
 *   2. 每期调用 HedgeFundOrchestrator 获取交易决策
 *   3. 执行交易，更新组合价值
 *   4. 计算 Sharpe / MaxDD / 年化收益等指标
 */
@Component
public class BacktestEngine {

    private static final Logger log = LoggerFactory.getLogger(BacktestEngine.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final HedgeFundOrchestrator orchestrator;
    private final FinancialDatasetsClient client;

    public BacktestEngine(HedgeFundOrchestrator orchestrator, FinancialDatasetsClient client) {
        this.orchestrator = orchestrator;
        this.client = client;
    }

    /**
     * 运行回测
     *
     * @param tickers       股票列表
     * @param startDate     回测起始日
     * @param endDate       回测结束日
     * @param initialCash   初始资金
     * @param modelName     LLM 模型名称
     * @param modelProvider LLM Provider
     * @return BacktestMetrics 绩效指标
     */
    public BacktestMetrics run(List<String> tickers, String startDate, String endDate,
                                double initialCash, String modelName, String modelProvider) {
        log.info("回测开始: tickers={}, {} → {}", tickers, startDate, endDate);

        LocalDate start = LocalDate.parse(startDate, FMT);
        LocalDate end = LocalDate.parse(endDate, FMT);
        int totalDays = (int) ChronoUnit.DAYS.between(start, end);

        // 按月生成决策日期（每月第一个工作日）
        List<LocalDate> decisionDates = generateMonthlyDates(start, end);

        BacktestPortfolio portfolio = new BacktestPortfolio(initialCash);
        List<Double> portfolioValues = new ArrayList<>();
        portfolioValues.add(initialCash);

        for (LocalDate date : decisionDates) {
            String dateStr = date.format(FMT);
            String periodStart = date.minusMonths(3).format(FMT);

            log.info("回测决策日: {}", dateStr);

            // 构建运行请求
            HedgeFundRunReq req = new HedgeFundRunReq();
            req.setTickers(tickers);
            req.setStartDate(periodStart);
            req.setEndDate(dateStr);
            req.setInitialCash(portfolio.getCash());
            req.setModelName(modelName != null ? modelName : "gpt-4o");
            req.setModelProvider(modelProvider != null ? modelProvider : "OpenAI");

            // 获取当日价格
            Map<String, Double> currentPrices = fetchCurrentPrices(tickers, dateStr);

            // 运行编排器获取决策
            WorkflowResult result = orchestrator.run(req, null);

            // 执行交易决策
            if (result.getDecisions() != null) {
                executeDecisions(portfolio, result.getDecisions(), currentPrices, portfolio.getCash());
            }

            // 记录组合价值
            double value = portfolio.getTotalValue(currentPrices);
            portfolioValues.add(value);
            log.info("  组合价值: ¥{:.2f}".replace("{:.2f}", String.format("%.2f", value)));
        }

        double[] values = portfolioValues.stream().mapToDouble(Double::doubleValue).toArray();
        BacktestMetrics metrics = BacktestMetrics.compute(values, totalDays);

        log.info("回测完成: 总收益={:.2f}%, Sharpe={:.2f}, MaxDD={:.2f}%"
                .replace("{:.2f}", "%.2f").formatted(
                        metrics.getTotalReturnPct(),
                        metrics.getSharpeRatio(),
                        metrics.getMaxDrawdownPct()));

        return metrics;
    }

    private List<LocalDate> generateMonthlyDates(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start.withDayOfMonth(1).plusMonths(1);
        while (!current.isAfter(end)) {
            dates.add(current);
            current = current.plusMonths(1);
        }
        return dates;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> fetchCurrentPrices(List<String> tickers, String date) {
        Map<String, Double> prices = new HashMap<>();
        String startDate = LocalDate.parse(date, FMT).minusDays(5).format(FMT);

        for (String ticker : tickers) {
            List<Map<String, Object>> bars = client.getPrices(ticker, startDate, date);
            if (bars != null && !bars.isEmpty()) {
                Object close = bars.get(bars.size() - 1).get("close");
                if (close instanceof Number) {
                    prices.put(ticker, ((Number) close).doubleValue());
                }
            }
        }
        return prices;
    }

    @SuppressWarnings("unchecked")
    private void executeDecisions(BacktestPortfolio portfolio,
                                   Map<String, Object> decisions,
                                   Map<String, Double> prices,
                                   double availableCash) {
        for (Map.Entry<String, Object> entry : decisions.entrySet()) {
            String ticker = entry.getKey();
            Map<String, Object> decision = (Map<String, Object>) entry.getValue();
            if (decision == null) continue;

            String action = String.valueOf(decision.getOrDefault("action", "hold"));
            double price = prices.getOrDefault(ticker, 0.0);
            if (price <= 0) continue;

            switch (action) {
                case "buy" -> {
                    // 最多用可用现金的 10% 买入
                    int qty = (int) (availableCash * 0.10 / price);
                    if (qty > 0 && portfolio.buy(ticker, qty, price)) {
                        log.debug("  买入 {} x {} @ {:.2f}".replace("{:.2f}", "%.2f").formatted(qty, ticker, price));
                    }
                }
                case "sell" -> {
                    int held = portfolio.getPositions().getOrDefault(ticker, 0);
                    if (held > 0 && portfolio.sell(ticker, held, price)) {
                        log.debug("  卖出 {} x {} @ {:.2f}".replace("{:.2f}", "%.2f").formatted(held, ticker, price));
                    }
                }
                default -> log.debug("  持仓不变: {}", ticker);
            }
        }
    }
}
