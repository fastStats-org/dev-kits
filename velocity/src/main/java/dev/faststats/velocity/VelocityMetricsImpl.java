package dev.faststats.velocity;

import com.google.gson.JsonObject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.core.Metrics;
import dev.faststats.core.SimpleMetrics;
import org.jetbrains.annotations.Async;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class VelocityMetricsImpl extends SimpleMetrics implements VelocityMetrics {
    private final Logger logger;
    private final ProxyServer server;
    private final PluginContainer plugin;

    private VelocityMetricsImpl(
            SimpleMetrics.Factory<?> factory,
            Logger logger,
            ProxyServer server,
            Path config,
            PluginContainer plugin
    ) throws IOException, IllegalStateException {
        super(factory, config);

        this.logger = logger;
        this.server = server;
        this.plugin = plugin;

        startSubmitting();
    }

    @Async.Schedule
    private void startSubmitting() {
        startSubmitting(0, 30, TimeUnit.MINUTES);
    }

    @Override
    protected void appendDefaultData(JsonObject charts) {
        var pluginVersion = plugin.getDescription().getVersion().orElse("unknown");
        var size = server.getPlayerCount();

        charts.addProperty("online_mode", server.getConfiguration().isOnlineMode());
        charts.addProperty("plugin_version", pluginVersion);
        charts.addProperty("server_type", server.getVersion().getName());
        charts.addProperty("proxy_version", server.getVersion().getVersion());
        charts.addProperty("proxy_vendor", server.getVersion().getVendor());
        if (size != 0) charts.addProperty("player_count", size);
    }

    @Override
    protected void error(String message, @Nullable Throwable throwable) {
        if (!isDebug()) return;
        var msg = "[" + VelocityMetricsImpl.class.getName() + "]: " + message;
        logger.error(msg, throwable);
    }

    @Override
    protected void warn(String message) {
        if (!isDebug()) return;
        var msg = "[" + VelocityMetricsImpl.class.getName() + "]: " + message;
        logger.warn(msg);
    }

    @Override
    protected void info(String message) {
        if (!isDebug()) return;
        var msg = "[" + VelocityMetricsImpl.class.getName() + "]: " + message;
        logger.info(msg);
    }

    private <T> Optional<T> tryOrEmpty(Supplier<@Nullable T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (NoSuchMethodError | Exception e) {
            error("Failed to call supplier", e);
            return Optional.empty();
        }
    }

    static class Factory extends SimpleMetrics.Factory<Object> {
        protected final Logger logger;
        protected final Path dataDirectory;
        protected final ProxyServer server;

        public Factory(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
            this.logger = logger;
            this.dataDirectory = dataDirectory;
            this.server = server;
        }

        @Override
        public Metrics create(Object plugin) throws IOException, IllegalStateException, IllegalArgumentException {
            var faststats = dataDirectory.resolveSibling("faststats");
            var container = server.getPluginManager().ensurePluginContainer(plugin);
            return new VelocityMetricsImpl(this, logger, server, faststats.resolve("config.json"), container);
        }
    }
}
