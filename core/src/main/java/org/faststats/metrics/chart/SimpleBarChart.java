package org.faststats.metrics.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;

public class SimpleBarChart extends Chart<Map<String, Number>> {
    public SimpleBarChart(String id, Callable<@Nullable Map<String, Number>> callable) {
        super(id, callable);
    }

    @Override
    public @Nullable JsonElement getValues() throws Exception {
        var bar = callable.call();
        if (bar == null || bar.isEmpty()) return null;

        var values = new JsonObject();
        bar.forEach(values::addProperty);
        return values;
    }
}
