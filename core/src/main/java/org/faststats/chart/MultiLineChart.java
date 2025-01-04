package org.faststats.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;

public class MultiLineChart extends Chart<Map<String, Number>> {
    public MultiLineChart(String id, Callable<@Nullable Map<String, Number>> callable) {
        super(id, callable);
    }

    @Override
    public @Nullable JsonElement getValues() throws Exception {
        var lines = callable.call();
        if (lines == null || lines.isEmpty()) return null;

        var values = new JsonObject();
        lines.forEach(values::addProperty);
        return values;
    }
}
