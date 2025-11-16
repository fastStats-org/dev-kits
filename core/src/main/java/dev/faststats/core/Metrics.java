package dev.faststats.core;

import dev.faststats.core.chart.Chart;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Metrics interface.
 *
 * @since 0.1.0
 */
public interface Metrics {
    /**
     * Get the token used to authenticate with the metrics server and identify the project.
     *
     * @return the metrics token
     * @since 0.1.0
     */
    @Token
    @Contract(pure = true)
    String getToken();

    /**
     * Get the metrics configuration.
     *
     * @return the metrics configuration
     * @since 0.1.0
     */
    @Contract(pure = true)
    Config getConfig();

    /**
     * Shuts down the metrics submission.
     * <p>
     * This method should be called when the application is shutting down.
     *
     * @since 0.1.0
     */
    @Contract(mutates = "this")
    void shutdown();

    /**
     * A metrics factory.
     *
     * @since 0.1.0
     */
    interface Factory {
        /**
         * Adds a chart to the metrics submission.
         *
         * @param chart the chart to add
         * @return the metrics factory
         * @throws IllegalArgumentException if the chart is already added
         * @since 0.1.0
         */
        @Contract(mutates = "this")
        Factory addChart(Chart<?> chart) throws IllegalArgumentException;

        /**
         * Enables or disabled debug mode for this metrics instance.
         * <p>
         * If {@link Config#debug()} is enabled, debug logging will be enabled for all metrics instances,
         * including this one, regardless of this setting.
         * <p>
         * This is only meant for development and testing and should not be enabled in production.
         *
         * @param enabled whether debug mode is enabled
         * @return the metrics factory
         * @since 0.1.0
         */
        @Contract(mutates = "this")
        Factory debug(boolean enabled);

        /**
         * Sets the token used to authenticate with the metrics server and identify the project.
         * <p>
         * This token can be found in the settings of your project under <b>"Your API Token"</b>.
         *
         * @param token the metrics token
         * @return the metrics factory
         * @throws IllegalArgumentException if the token does not match the {@link Token#PATTERN}
         * @since 0.1.0
         */
        @Contract(mutates = "this")
        Factory token(@Token String token) throws IllegalArgumentException;

        /**
         * Sets the metrics server URL.
         * <p>
         * This is only required for self-hosted metrics servers.
         *
         * @param url the metrics server URL
         * @return the metrics factory
         * @since 0.1.0
         */
        @Contract(mutates = "this")
        Factory url(URI url);

        /**
         * Creates a new metrics instance.
         * <p>
         * Metrics submission will start automatically.
         *
         * @return the metrics instance
         * @throws IOException           if the config file cannot be read or written
         * @throws IllegalStateException if the token is not specified
         * @see #token(String)
         * @since 0.1.0
         */
        @Async.Schedule
        @Contract(value = " -> new", mutates = "io")
        Metrics create() throws IOException, IllegalStateException;
    }

    /**
     * A representation of the metrics configuration.
     *
     * @since 0.1.0
     */
    interface Config {
        /**
         * The server id.
         *
         * @return the server id
         * @since 0.1.0
         */
        @Contract(pure = true)
        UUID serverId();

        /**
         * Whether metrics submission is enabled.
         * <p>
         * <b>Bypassing this setting may get your project banned from FastStats.</b><br>
         * <b>Users have to be able to opt out from metrics submission.</b>
         *
         * @return true if metrics submission is enabled, false otherwise
         * @since 0.1.0
         */
        @Contract(pure = true)
        boolean enabled();

        /**
         * Whether debug logging is enabled across all metrics instances.
         *
         * @return true if debug logging is enabled, false otherwise
         * @since 0.1.0
         */
        @Contract(pure = true)
        boolean debug();
    }
}
