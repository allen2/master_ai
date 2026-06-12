package com.aihedgefund.backtesting;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BacktestMetricsTest {

    @Test
    void compute_simpleGrowth_correctReturn() {
        double[] values = {100_000, 105_000, 110_000, 115_000};
        BacktestMetrics m = BacktestMetrics.compute(values, 90);
        assertThat(m.getTotalReturnPct()).isEqualTo(15.0, within(0.01));
        assertThat(m.getInitialValue()).isEqualTo(100_000.0, within(0.01));
        assertThat(m.getFinalValue()).isEqualTo(115_000.0, within(0.01));
    }

    @Test
    void compute_withDrawdown_maxDdCalculated() {
        // Peak at 110k, trough at 90k → MaxDD = 18.18%
        double[] values = {100_000, 110_000, 90_000, 95_000};
        BacktestMetrics m = BacktestMetrics.compute(values, 60);
        assertThat(m.getMaxDrawdownPct()).isGreaterThan(15.0);
    }

    @Test
    void compute_nullValues_returnsZero() {
        BacktestMetrics m = BacktestMetrics.compute(null, 0);
        assertThat(m.getTotalReturnPct()).isEqualTo(0.0);
    }

    @Test
    void compute_positiveReturns_positiveSharpe() {
        double[] values = new double[252];
        values[0] = 100_000;
        for (int i = 1; i < 252; i++) values[i] = values[i - 1] * 1.001; // +0.1% daily
        BacktestMetrics m = BacktestMetrics.compute(values, 252);
        assertThat(m.getSharpeRatio()).isGreaterThan(0);
        assertThat(m.getAnnualizedReturnPct()).isGreaterThan(0);
    }
}
