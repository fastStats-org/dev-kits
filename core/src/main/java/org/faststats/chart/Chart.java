package org.faststats.chart;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    public abstract @Nullable JsonElement getValues() throws Exception;

    public final @Nullable JsonObject getData() throws Exception {
        var values = getValues();
        if (values == null) return null;
        var object = new JsonObject();
        object.addProperty("id", this.id);
        object.add("values", values);
        return object;
    }

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
