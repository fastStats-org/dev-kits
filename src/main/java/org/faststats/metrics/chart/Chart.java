package org.faststats.metrics.chart;

import com.google.gson.JsonElement;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Chart {
    JsonElement getData();
    String getId();
}
