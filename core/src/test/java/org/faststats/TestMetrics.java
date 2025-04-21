package org.faststats;

import org.faststats.chart.SimplePieChart;
import org.faststats.chart.SingleLineChart;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@NullMarked
public class TestMetrics extends Metrics {
    private final Logger logger = Logger.getLogger(getClass().getName());

    public TestMetrics(UUID consumerId, boolean enabled, int projectId) {
        super(consumerId, enabled, projectId);
        addChart(new SimplePieChart("online_mode", () -> String.valueOf(true)));
        addChart(new SimplePieChart("plugin_version", () -> "0.1.0"));
        addChart(new SimplePieChart("server_type", () -> "Paper"));
        addChart(new SimplePieChart("server_version", () -> "1.21.4"));
        addChart(new SingleLineChart("player_amount", () -> 52));
        startSubmitting();
    }

    @Override
    protected void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    @Override
    protected String getURL() {
        return "http://localhost:5000/metrics";
    }

    public static void main(String[] args) {
        new TestMetrics(UUID.randomUUID(), true, 12345);
        while (true);
    }
}
