package dev.faststats.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Callable;

final class SingleValueChart<T> extends SimpleChart<T> {
    public SingleValueChart(@ChartId String id, Callable<@Nullable T> callable) {
        super(id, callable);
    }

    @Override
    public Optional<JsonElement> getData() throws Exception {
        return compute().map(data -> switch (data) {
            case Boolean bool -> new JsonPrimitive(bool);
            case Number number -> new JsonPrimitive(number);
            default -> new JsonPrimitive(data.toString());
        });
    }
}
