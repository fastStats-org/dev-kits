package dev.faststats.bukkit;

import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import dev.faststats.core.chart.Chart;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

final class BukkitMetricsImpl extends SimpleMetrics implements BukkitMetrics {
    private final Plugin plugin;
    private final boolean onlineMode;

    private BukkitMetricsImpl(SimpleMetrics.Factory factory, Plugin plugin, Path config) throws IOException, IllegalStateException {
        super(factory, config);

        this.plugin = plugin;
        this.onlineMode = checkOnlineMode();

        setup(factory);
        startSubmitting();
    }

    @Contract(mutates = "param")
    @SuppressWarnings("deprecation")
    private void setup(Metrics.Factory factory) {
        factory.addChart(Chart.bool("online_mode", () -> onlineMode));
        factory.addChart(Chart.string("plugin_version", () -> {
            return tryOrEmpty(() -> plugin.getPluginMeta().getVersion())
                    .orElseGet(() -> plugin.getDescription().getVersion());
        }));
        factory.addChart(Chart.string("server_type", () -> plugin.getServer().getName()));
        factory.addChart(Chart.string("minecraft_version", () -> {
            return tryOrEmpty(() -> plugin.getServer().getMinecraftVersion())
                    .orElse("unknown");
        }));
        factory.addChart(Chart.number("player_count", () -> {
            var size = plugin.getServer().getOnlinePlayers().size();
            return size != 0 ? size : null;
        }));
    }

    @Async.Schedule
    private void startSubmitting() {
        startSubmitting(0, 30, TimeUnit.MINUTES);
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

    @Override
    protected void error(String message, @Nullable Throwable throwable) {
        if (!isDebug()) return;
        var msg = "[" + BukkitMetricsImpl.class.getName() + "]: " + message;
        plugin.getLogger().log(Level.SEVERE, msg, throwable);
    }

    @Override
    protected void warn(String message) {
        if (!isDebug()) return;
        var msg = "[" + BukkitMetricsImpl.class.getName() + "]: " + message;
        plugin.getLogger().log(Level.WARNING, msg);
    }

    @Override
    protected void info(String message) {
        if (!isDebug()) return;
        var msg = "[" + BukkitMetricsImpl.class.getName() + "]: " + message;
        plugin.getLogger().log(Level.INFO, msg);
    }

    private <T> Optional<T> tryOrEmpty(Supplier<@Nullable T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (NoSuchMethodError | Exception e) {
            error("Failed to call supplier", e);
            return Optional.empty();
        }
    }

    static final class Factory extends SimpleMetrics.Factory {
        private final Plugin plugin;
        private final Path config;

        public Factory(Plugin plugin, Path config) {
            this.plugin = plugin;
            this.config = config;
        }

        @Override
        public Metrics create() throws IOException, IllegalStateException {
            return new BukkitMetricsImpl(this, plugin, config);
        }
    }
}
