package org.faststats.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

public class SingleLineChart extends Chart<Number> {
    public SingleLineChart(String id, Callable<@Nullable Number> callable) {
        super(id, callable);
    }

    @Override
    public @Nullable JsonElement getData() throws Exception {
        var value = callable.call();
        return value != null ? new JsonPrimitive(value) : null;
    }
}
