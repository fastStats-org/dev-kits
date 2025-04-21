package org.faststats.chart;

import com.google.gson.JsonElement;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

@NullMarked
public abstract class Chart<T> {
    protected final String id;
    protected final Callable<@Nullable T> callable;

    public Chart(String id, Callable<@Nullable T> callable) {
        this.id = id;
        this.callable = callable;
    }

    public abstract @Nullable JsonElement getData() throws Exception;

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Chart{" +
               "id='" + id + '\'' +
               '}';
    }
}
