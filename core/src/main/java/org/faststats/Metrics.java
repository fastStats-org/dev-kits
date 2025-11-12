package org.faststats;

import org.faststats.chart.Chart;

public interface Metrics {
    String getToken();

    void addChart(Chart<?> chart);
}
