package dev.faststats.bukkit;

import com.google.gson.JsonObject;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Async;
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
    private final Plugin plugin;

    private BukkitMetricsImpl(SimpleMetrics.Factory<?> factory, Plugin plugin, Path config) throws IOException, IllegalStateException {
        super(factory, config);

        this.logger = plugin.getLogger();
        this.server = plugin.getServer();
        this.plugin = plugin;

        startSubmitting();
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
    @SuppressWarnings("deprecation")
    protected void appendDefaultData(JsonObject charts) {
        var pluginVersion = tryOrEmpty(() -> plugin.getPluginMeta().getVersion())
                .orElseGet(() -> plugin.getDescription().getVersion());
        var minecraftVersion = tryOrEmpty(server::getMinecraftVersion)
                .orElse("unknown"); // fixme: bukkit compat
        var players = server.getOnlinePlayers().size();

        charts.addProperty("online_mode", checkOnlineMode());
        charts.addProperty("plugin_version", pluginVersion);
        charts.addProperty("server_type", server.getName());
        charts.addProperty("minecraft_version", minecraftVersion);
        if (players != 0) charts.addProperty("player_count", players);
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

    static final class Factory extends SimpleMetrics.Factory<Plugin> {
        @Override
        public Metrics create(Plugin plugin) throws IOException, IllegalStateException {
            var dataFolder = plugin.getServer().getPluginsFolder().toPath().resolve("faststats");
            var config = dataFolder.resolve("config.json");
            return new BukkitMetricsImpl(this, plugin, config);
        }
    }
}
