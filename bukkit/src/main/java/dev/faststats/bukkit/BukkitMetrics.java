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
     * <p>
     * The config file is usually located at <code>plugins/faststats/config.json</code>.
     *
     * @param plugin the Bukkit plugin
     * @return the metrics factory
     * @since 0.1.0
     */
    @Contract(pure = true)
    static Metrics.Factory factory(Plugin plugin) {
        var dataFolder = plugin.getServer().getPluginsFolder().toPath().resolve("faststats");
        var config = dataFolder.resolve("config.json");
        return new BukkitMetricsImpl.Factory(plugin, config);
    }
}
