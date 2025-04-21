package org.faststats.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;

@NullMarked
public class ComplexPieChart extends Chart<Map<String, Number>> {
    public ComplexPieChart(String id, Callable<@Nullable Map<String, Number>> callable) {
        super(id, callable);
    }

    @Override
    public @Nullable JsonElement getData() throws Exception {
        var pie = callable.call();
        if (pie == null || pie.isEmpty()) return null;

        var values = new JsonObject();
        pie.forEach(values::addProperty);
        return values;
    }
}
