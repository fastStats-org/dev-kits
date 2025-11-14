package org.faststats.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Callable;

final class SimpleLineChart extends SimpleChart<Number> {
    public SimpleLineChart(@ChartId String id, Callable<@Nullable Number> callable) {
        super(id, callable);
    }

    @Override
    public Optional<JsonElement> getData() throws Exception {
        return compute().filter(number -> number.doubleValue() != 0).map(JsonPrimitive::new);
    }
}
