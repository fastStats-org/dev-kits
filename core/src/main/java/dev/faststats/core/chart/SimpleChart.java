package dev.faststats.core.chart;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Callable;

abstract class SimpleChart<T> implements Chart<T> {
    private final @ChartId String id;
    private final Callable<@Nullable T> callable;

    public SimpleChart(@ChartId String id, Callable<@Nullable T> callable) throws IllegalArgumentException {
        if (!id.matches(ChartId.PATTERN)) {
            throw new IllegalArgumentException("Invalid chart id '" + id + "', must match '" + ChartId.PATTERN + "'");
        }
        this.id = id;
        this.callable = callable;
    }

    @Override
    public final @ChartId String getId() {
        return id;
    }

    public final Optional<T> compute() throws Exception {
        return Optional.ofNullable(callable.call());
    }

    @Override
    public String toString() {
        return "SimpleChart{" +
                "id='" + id + '\'' +
                '}';
    }
}
