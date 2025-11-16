package dev.faststats.core;

import dev.faststats.core.chart.Chart;
import org.jetbrains.annotations.Contract;

public interface Metrics {
    @Contract(pure = true)
    String getToken();

    @Contract(mutates = "this")
    void addChart(Chart<?> chart);
}
