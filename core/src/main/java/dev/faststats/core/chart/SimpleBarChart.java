package dev.faststats.core.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@Deprecated
final class SimpleBarChart extends SimpleChart<Map<String, Number>> {
    public SimpleBarChart(@ChartId String id, Callable<@Nullable Map<String, Number>> callable) throws IllegalArgumentException {
        super(id, callable);
    }

    @Override
    public Optional<JsonElement> getData() throws Exception {
        return compute().filter(bar -> !bar.isEmpty()).map(bar -> {
            var values = new JsonObject();
            bar.forEach(values::addProperty);
            return values;
        });
    }
}
