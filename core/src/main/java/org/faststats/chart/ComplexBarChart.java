package org.faststats.chart;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;

@NullMarked
public class ComplexBarChart extends Chart<Map<String, Number[]>> {
    public ComplexBarChart(String id, Callable<@Nullable Map<String, Number[]>> callable) {
        super(id, callable);
    }

    @Override
    public @Nullable JsonElement getValues() throws Exception {
        var bar = callable.call();
        if (bar == null || bar.isEmpty()) return null;

        var list = bar.entrySet().stream()
                .filter(entry -> entry.getValue().length > 0)
                .toList();
        if (list.isEmpty()) return null;

        var values = new JsonObject();
        list.forEach(entry -> {
            var array = entry.getValue();
            var elements = new JsonArray(array.length);
            for (var i : array) elements.add(i);
            values.add(entry.getKey(), elements);
        });
        return values;
    }
}
