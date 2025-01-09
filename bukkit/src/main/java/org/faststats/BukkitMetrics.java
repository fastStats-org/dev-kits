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
    private final boolean onlineMode;

    @SuppressWarnings("deprecation")
    public BukkitMetrics(Plugin plugin, int projectId) {
        super(UUID.randomUUID() /* todo: faststats save file*/, true, projectId);
        this.onlineMode = checkOnlineMode();
        addChart(new SimplePieChart("online_mode", () -> String.valueOf(onlineMode)));
        addChart(new SimplePieChart("plugin_version", () -> plugin.getDescription().getVersion()));
        addChart(new SimplePieChart("server_type", () -> plugin.getServer().getName()));
        addChart(new SimplePieChart("server_version", () -> plugin.getServer().getMinecraftVersion()));
        addChart(new SingleLineChart("player_amount", () -> plugin.getServer().getOnlinePlayers().size()));
        this.plugin = plugin;
    }

    private boolean checkOnlineMode() {
        return plugin.getServer().getOnlineMode();
    }

    @Override
    protected void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }
}
