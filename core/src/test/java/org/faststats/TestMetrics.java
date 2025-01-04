package org.faststats;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@NullMarked
public class TestMetrics extends Metrics {
    private final Logger logger = Logger.getLogger(getClass().getName());

    public TestMetrics(UUID consumerId, boolean enabled, int projectId) {
        super(consumerId, enabled, projectId);
    }

    @Override
    protected void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    @Override
    protected String getURL() {
        return "localhost:3000/metrics";
    }

    public static void main(String[] args) {
        new TestMetrics(UUID.randomUUID(), true, 12345);
        while (true);
    }
}
