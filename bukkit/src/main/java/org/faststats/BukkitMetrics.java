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
        this.plugin = plugin;
        addChart(new SimplePieChart("online_mode", () -> String.valueOf(onlineMode)));
        addChart(new SimplePieChart("plugin_version", () -> plugin.getDescription().getVersion()));
        addChart(new SimplePieChart("server_type", () -> plugin.getServer().getName()));
        addChart(new SimplePieChart("server_version", () -> plugin.getServer().getMinecraftVersion()));
        addChart(new SingleLineChart("player_amount", () -> plugin.getServer().getOnlinePlayers().size()));
        startSubmitting();
    }

    @SuppressWarnings("removal")
    private boolean checkOnlineMode() {
        try {
            if (plugin.getServer().getOnlineMode()) return true;

            // waiting for https://github.com/PaperMC/Paper/pull/12273

            var proxies = plugin.getServer().spigot().getPaperConfig().getConfigurationSection("proxies");
            if (proxies == null) return false;

            if (proxies.getBoolean("velocity.enabled") && proxies.getBoolean("velocity.online-mode")) return true;

            var settings = plugin.getServer().spigot().getSpigotConfig().getConfigurationSection("settings");
            if (settings == null) return false;

            return settings.getBoolean("bungeecord") && proxies.getBoolean("bungee-cord.online-mode");
        } catch (NoSuchMethodError e) {
            return plugin.getServer().getOnlineMode();
        }
    }

    @Override
    protected void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }
}
