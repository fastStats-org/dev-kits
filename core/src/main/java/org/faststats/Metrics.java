package org.faststats;

import org.faststats.chart.Chart;

public interface Metrics {
    int getProjectId();

    void addChart(Chart<?> chart);
}
