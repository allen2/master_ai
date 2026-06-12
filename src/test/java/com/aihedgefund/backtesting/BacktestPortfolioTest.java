package com.aihedgefund.backtesting;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class BacktestPortfolioTest {

    @Test
    void buy_sufficientCash_reducesBalance() {
        BacktestPortfolio portfolio = new BacktestPortfolio(10_000);
        boolean ok = portfolio.buy("AAPL", 10, 100.0);
        assertThat(ok).isTrue();
        assertThat(portfolio.getCash()).isEqualTo(9_000.0, org.assertj.core.api.Assertions.within(0.01));
        assertThat(portfolio.getPositions().get("AAPL")).isEqualTo(10);
    }

    @Test
    void buy_insufficientCash_returnsFalse() {
        BacktestPortfolio portfolio = new BacktestPortfolio(500);
        boolean ok = portfolio.buy("AAPL", 10, 100.0);
        assertThat(ok).isFalse();
        assertThat(portfolio.getCash()).isEqualTo(500.0);
    }

    @Test
    void sell_existingPosition_calculatesGain() {
        BacktestPortfolio portfolio = new BacktestPortfolio(10_000);
        portfolio.buy("AAPL", 10, 100.0);     // cost = 1000
        boolean ok = portfolio.sell("AAPL", 10, 150.0); // proceeds = 1500
        assertThat(ok).isTrue();
        assertThat(portfolio.getTotalRealizedGains()).isEqualTo(500.0, org.assertj.core.api.Assertions.within(0.01));
        assertThat(portfolio.getPositions()).doesNotContainKey("AAPL");
    }

    @Test
    void sell_noPosition_returnsFalse() {
        BacktestPortfolio portfolio = new BacktestPortfolio(10_000);
        boolean ok = portfolio.sell("AAPL", 5, 100.0);
        assertThat(ok).isFalse();
    }

    @Test
    void getTotalValue_includesCashAndPositions() {
        BacktestPortfolio portfolio = new BacktestPortfolio(10_000);
        portfolio.buy("AAPL", 10, 100.0); // cash = 9000, positions = 10 * price
        double value = portfolio.getTotalValue(Map.of("AAPL", 120.0));
        assertThat(value).isEqualTo(10_200.0, org.assertj.core.api.Assertions.within(0.01)); // 9000 + 10*120
    }
}
