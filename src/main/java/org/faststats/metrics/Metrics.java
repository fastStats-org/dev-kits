package org.faststats.metrics;

import com.google.gson.JsonObject;
import org.faststats.metrics.chart.Chart;

import java.util.HashSet;
import java.util.Set;

public class Metrics {
    private final JsonObject data = new JsonObject();
    private final Set<Chart> charts = new HashSet<>();

    public void add(String key, String value) {
        data.addProperty(key, value);
    }
}
