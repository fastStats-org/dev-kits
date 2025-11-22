package dev.faststats.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.core.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Bukkit metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface VelocityMetrics extends Metrics permits VelocityMetricsImpl {
    final class Factory extends VelocityMetricsImpl.Factory {
        /**
         * Creates a new metrics factory for Velocity.
         *
         * @param server        the velocity server
         * @param logger        the logger
         * @param dataDirectory the data directory
         * @apiNote This instance is automatically injected into your plugin.
         * @since 0.1.0
         */
        @Inject
        private Factory(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
            super(server, logger, dataDirectory);
        }
    }
}
