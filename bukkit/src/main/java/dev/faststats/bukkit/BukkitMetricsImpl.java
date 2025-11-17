package dev.faststats.bukkit;

import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import dev.faststats.core.chart.Chart;
import org.bukkit.Server;
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
import java.util.logging.Logger;

final class BukkitMetricsImpl extends SimpleMetrics implements BukkitMetrics {
    private final Logger logger;
    private final Server server;

    private BukkitMetricsImpl(SimpleMetrics.Factory factory, Plugin plugin, Path config) throws IOException, IllegalStateException {
        super(factory, config);

        this.logger = plugin.getLogger();
        this.server = plugin.getServer();

        setup(factory, plugin);
        startSubmitting();
    }

    @Contract(mutates = "param")
    @SuppressWarnings("deprecation")
    private void setup(SimpleMetrics.Factory factory, Plugin plugin) throws IllegalArgumentException {
        var pluginVersion = tryOrEmpty(() -> plugin.getPluginMeta().getVersion())
                .orElseGet(() -> plugin.getDescription().getVersion());
        var minecraftVersion = tryOrEmpty(server::getMinecraftVersion)
                .orElse("unknown"); // fixme: bukkit compat
        var onlineMode = checkOnlineMode();

        addChart(Chart.bool("online_mode", () -> onlineMode));
        addChart(Chart.string("plugin_version", () -> pluginVersion));
        addChart(Chart.string("server_type", server::getName));
        addChart(Chart.string("minecraft_version", () -> minecraftVersion));
        addChart(Chart.number("player_count", () -> {
            var size = server.getOnlinePlayers().size();
            return size != 0 ? size : null;
        }));
    }

    @Async.Schedule
    private void startSubmitting() {
        startSubmitting(0, 30, TimeUnit.MINUTES);
    }

    private boolean checkOnlineMode() {
        return tryOrEmpty(server.getServerConfig()::isProxyOnlineMode)
                .or(() -> tryOrEmpty(this::isProxyOnlineMode))
                .orElseGet(server::getOnlineMode);
    }

    @SuppressWarnings("removal")
    private boolean isProxyOnlineMode() {
        var proxies = server.spigot().getPaperConfig().getConfigurationSection("proxies");
        if (proxies == null) return false;

        if (proxies.getBoolean("velocity.enabled") && proxies.getBoolean("velocity.online-mode")) return true;

        var settings = server.spigot().getSpigotConfig().getConfigurationSection("settings");
        if (settings == null) return false;

        return settings.getBoolean("bungeecord") && proxies.getBoolean("bungee-cord.online-mode");
    }

    @Override
    protected void error(String message, @Nullable Throwable throwable) {
        if (!isDebug()) return;
        var msg = "[" + BukkitMetricsImpl.class.getName() + "]: " + message;
        logger.log(Level.SEVERE, msg, throwable);
    }

    @Override
    protected void warn(String message) {
        if (!isDebug()) return;
        var msg = "[" + BukkitMetricsImpl.class.getName() + "]: " + message;
        logger.log(Level.WARNING, msg);
    }

    @Override
    protected void info(String message) {
        if (!isDebug()) return;
        var msg = "[" + BukkitMetricsImpl.class.getName() + "]: " + message;
        logger.log(Level.INFO, msg);
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
