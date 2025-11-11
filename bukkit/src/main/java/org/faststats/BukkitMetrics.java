package org.faststats;

import org.bukkit.plugin.Plugin;
import org.faststats.chart.Chart;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

@NullMarked
public class BukkitMetrics extends SimpleMetrics {
    private final Plugin plugin;
    private final boolean onlineMode;
    private final boolean debug;

    @SuppressWarnings("deprecation")
    public BukkitMetrics(Plugin plugin, int projectId, boolean debug) {
        super(UUID.randomUUID() /* todo: faststats save file*/, true, projectId);

        this.debug = debug;
        this.plugin = plugin;
        this.onlineMode = checkOnlineMode();

        addChart(Chart.pie("online_mode", () -> String.valueOf(onlineMode)));
        addChart(Chart.pie("plugin_version", () -> plugin.getDescription().getVersion()));
        addChart(Chart.pie("server_type", () -> plugin.getServer().getName()));
        addChart(Chart.pie("server_version", () -> plugin.getServer().getMinecraftVersion()));
        addChart(Chart.line("player_amount", () -> plugin.getServer().getOnlinePlayers().size()));
        startSubmitting();
    }

    private boolean checkOnlineMode() {
        return tryOrEmpty(plugin.getServer().getServerConfig()::isProxyOnlineMode)
                .or(() -> tryOrEmpty(this::isProxyOnlineMode))
                .orElseGet(plugin.getServer()::getOnlineMode);
    }

    @SuppressWarnings("removal")
    private boolean isProxyOnlineMode() {
        var proxies = plugin.getServer().spigot().getPaperConfig().getConfigurationSection("proxies");
        if (proxies == null) return false;

        if (proxies.getBoolean("velocity.enabled") && proxies.getBoolean("velocity.online-mode")) return true;

        var settings = plugin.getServer().spigot().getSpigotConfig().getConfigurationSection("settings");
        if (settings == null) return false;

        return settings.getBoolean("bungeecord") && proxies.getBoolean("bungee-cord.online-mode");
    }

    private <T> Optional<T> tryOrEmpty(Supplier<@Nullable T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (NoSuchMethodError | Exception e) {
            return Optional.empty();
        }
    }

    @Override
    protected void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }

    @Override
    protected void debug(String message, Throwable throwable) {
        if (debug) plugin.getLogger().log(Level.WARNING, message, throwable);
    }
}
