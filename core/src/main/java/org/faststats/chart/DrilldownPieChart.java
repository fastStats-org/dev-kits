package org.faststats.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;

public class DrilldownPieChart extends Chart<Map<String, Map<String, Number>>> {
    public DrilldownPieChart(String id, Callable<@Nullable Map<String, Map<String, Number>>> callable) {
        super(id, callable);
    }

    @Override
    public @Nullable JsonElement getData() throws Exception {
        var drilldown = callable.call();
        if (drilldown == null || drilldown.isEmpty()) return null;

        var values = new JsonObject();
        drilldown.forEach((key, value) -> {
            if (value.isEmpty()) return;
            var pie = new JsonObject();
            value.forEach(pie::addProperty);
            values.add(key, pie);
        });
        return values.isEmpty() ? null : values;
    }
}
