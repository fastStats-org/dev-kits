package org.faststats.chart;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * A chart.
 *
 * @param <T> the chart data type
 * @since 0.1.0
 */
public interface Chart<T> {
    /**
     * Get the chart id.
     *
     * @return the chart id
     * @since 0.1.0
     */
    @ChartId
    @Contract(pure = true)
    String getId();

    /**
     * Compute the chart data.
     *
     * @return an optional containing the chart data
     * @throws Exception if unable to compute the chart data
     * @implNote The implementation must be thread-safe and pure.
     * @since 0.1.0
     */
    @Contract(pure = true)
    Optional<T> compute() throws Exception;

    /**
     * Get the chart data as a JSON element.
     *
     * @return an optional containing the chart data as {@link JsonElement}
     * @throws Exception if unable to get the chart data
     * @implSpec The implementation must call {@link #compute()} to get the chart data
     * and follow the same thread-safety and pureness requirements.
     * @see #compute()
     * @since 0.1.0
     */
    @Contract(pure = true)
    Optional<JsonElement> getData() throws Exception;

    /**
     * Create a bar chart.
     *
     * @param id       the chart id
     * @param callable the chart data callable
     * @return the bar chart
     * @since 0.1.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Chart<Map<String, Number>> bar(@ChartId String id, Callable<@Nullable Map<String, Number>> callable) {
        return new SimpleBarChart(id, callable);
    }

    /**
     * Create a line chart.
     *
     * @param id       the chart id
     * @param callable the chart data callable
     * @return the line chart
     * @since 0.1.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Chart<Number> line(@ChartId String id, Callable<@Nullable Number> callable) {
        return new SimpleLineChart(id, callable);
    }

    /**
     * Create a pie chart.
     *
     * @param id       the chart id
     * @param callable the chart data callable
     * @return the pie chart
     * @since 0.1.0
     */
    @Contract(value = "_, _ -> new", pure = true)
    static Chart<String> pie(@ChartId String id, Callable<@Nullable String> callable) {
        return new SimplePieChart(id, callable);
    }
}
