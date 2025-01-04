package org.faststats;

import org.bukkit.plugin.Plugin;
import org.faststats.chart.SimplePieChart;
import org.faststats.chart.SingleLineChart;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;
import java.util.logging.Level;

@NullMarked
public class BukkitMetrics extends Metrics {
    private final Plugin plugin;

    @SuppressWarnings("deprecation")
    public BukkitMetrics(Plugin plugin, int projectId) {
        super(UUID.randomUUID() /* todo: faststats save file*/, true, projectId);
        addChart(new SimplePieChart("onlineMode", () -> String.valueOf(plugin.getServer().getOnlineMode())));
        addChart(new SimplePieChart("pluginVersion", () -> plugin.getDescription().getVersion()));
        addChart(new SimplePieChart("serverType", () -> plugin.getServer().getName()));
        addChart(new SimplePieChart("serverVersion", () -> plugin.getServer().getMinecraftVersion()));
        addChart(new SingleLineChart("playerAmount", () -> plugin.getServer().getOnlinePlayers().size()));
        this.plugin = plugin;
    }

    @Override
    protected void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }
}
