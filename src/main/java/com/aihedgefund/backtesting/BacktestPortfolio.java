package com.aihedgefund.backtesting;

import java.util.HashMap;
import java.util.Map;

/**
 * 回测期间的投资组合状态
 * 替代 Python backtesting/portfolio.py 的 Portfolio 类
 */
public class BacktestPortfolio {

    private double cash;
    private final Map<String, Integer> positions = new HashMap<>();   // ticker → 持仓数量
    private final Map<String, Double> costBasis = new HashMap<>();    // ticker → 平均成本
    private double totalRealizedGains = 0.0;

    public BacktestPortfolio(double initialCash) {
        this.cash = initialCash;
    }

    public boolean buy(String ticker, int quantity, double price) {
        double cost = quantity * price;
        if (cost > cash) return false;

        cash -= cost;
        int current = positions.getOrDefault(ticker, 0);
        double currentCost = costBasis.getOrDefault(ticker, 0.0);

        // 更新平均成本
        if (current == 0) {
            costBasis.put(ticker, price);
        } else {
            double totalCost = currentCost * current + cost;
            costBasis.put(ticker, totalCost / (current + quantity));
        }
        positions.put(ticker, current + quantity);
        return true;
    }

    public boolean sell(String ticker, int quantity, double price) {
        int current = positions.getOrDefault(ticker, 0);
        if (current < quantity) return false;

        double avgCost = costBasis.getOrDefault(ticker, 0.0);
        double gain = (price - avgCost) * quantity;
        totalRealizedGains += gain;
        cash += quantity * price;

        if (current == quantity) {
            positions.remove(ticker);
            costBasis.remove(ticker);
        } else {
            positions.put(ticker, current - quantity);
        }
        return true;
    }

    public double getTotalValue(Map<String, Double> currentPrices) {
        double positionValue = positions.entrySet().stream()
                .mapToDouble(e -> e.getValue() * currentPrices.getOrDefault(e.getKey(), 0.0))
                .sum();
        return cash + positionValue;
    }

    public double getCash() { return cash; }
    public Map<String, Integer> getPositions() { return new HashMap<>(positions); }
    public double getTotalRealizedGains() { return totalRealizedGains; }
}
