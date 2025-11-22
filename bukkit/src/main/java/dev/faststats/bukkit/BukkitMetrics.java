package dev.faststats.bukkit;

import dev.faststats.core.Metrics;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;

/**
 * Bukkit metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface BukkitMetrics extends Metrics permits BukkitMetricsImpl {
    /**
     * Creates a new metrics factory for Bukkit.
     *
     * @return the metrics factory
     * @since 0.1.0
     */
    @Contract(pure = true)
    static Metrics.Factory<Plugin> factory() {
        return new BukkitMetricsImpl.Factory();
    }
}
