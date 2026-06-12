package com.aihedgefund.backtesting;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 回测绩效指标
 * 替代 Python backtesting/metrics.py
 */
public class BacktestMetrics {

    @JsonProperty("total_return_pct")
    private double totalReturnPct;

    @JsonProperty("annualized_return_pct")
    private double annualizedReturnPct;

    @JsonProperty("sharpe_ratio")
    private double sharpeRatio;

    @JsonProperty("max_drawdown_pct")
    private double maxDrawdownPct;

    @JsonProperty("initial_value")
    private double initialValue;

    @JsonProperty("final_value")
    private double finalValue;

    @JsonProperty("total_days")
    private int totalDays;

    public BacktestMetrics() {}

    public static BacktestMetrics compute(double[] portfolioValues, int totalDays) {
        BacktestMetrics m = new BacktestMetrics();
        if (portfolioValues == null || portfolioValues.length < 2) return m;

        m.initialValue = portfolioValues[0];
        m.finalValue = portfolioValues[portfolioValues.length - 1];
        m.totalDays = totalDays;

        // Total return
        m.totalReturnPct = (m.finalValue - m.initialValue) / m.initialValue * 100;

        // Annualized return
        if (totalDays > 0) {
            double years = totalDays / 252.0;
            m.annualizedReturnPct = (Math.pow(m.finalValue / m.initialValue, 1.0 / years) - 1) * 100;
        }

        // Daily returns for Sharpe & MaxDD
        double[] dailyReturns = new double[portfolioValues.length - 1];
        double maxVal = portfolioValues[0];
        double maxDD = 0;

        for (int i = 1; i < portfolioValues.length; i++) {
            if (portfolioValues[i - 1] > 0) {
                dailyReturns[i - 1] = (portfolioValues[i] - portfolioValues[i - 1]) / portfolioValues[i - 1];
            }
            maxVal = Math.max(maxVal, portfolioValues[i]);
            double dd = (maxVal - portfolioValues[i]) / maxVal;
            maxDD = Math.max(maxDD, dd);
        }

        m.maxDrawdownPct = maxDD * 100;
        m.sharpeRatio = computeSharpe(dailyReturns);
        return m;
    }

    private static double computeSharpe(double[] returns) {
        if (returns.length == 0) return 0;
        double mean = 0;
        for (double r : returns) mean += r;
        mean /= returns.length;

        double variance = 0;
        for (double r : returns) variance += Math.pow(r - mean, 2);
        variance /= returns.length;
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) return 0;
        return (mean / stdDev) * Math.sqrt(252); // annualized
    }

    public double getTotalReturnPct() { return totalReturnPct; }
    public double getAnnualizedReturnPct() { return annualizedReturnPct; }
    public double getSharpeRatio() { return sharpeRatio; }
    public double getMaxDrawdownPct() { return maxDrawdownPct; }
    public double getInitialValue() { return initialValue; }
    public double getFinalValue() { return finalValue; }
    public int getTotalDays() { return totalDays; }
}
