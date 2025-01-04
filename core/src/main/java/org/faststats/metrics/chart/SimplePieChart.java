package org.faststats.metrics.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

@NullMarked
public class SimplePieChart extends Chart<String> {

    public SimplePieChart(String id, Callable<@Nullable String> callable) {
        super(id, callable);
    }

    @Override
    public @Nullable JsonElement getValues() throws Exception {
        var value = callable.call();
        return value != null ? new JsonPrimitive(value) : null;
    }
}
