package dev.faststats.core;

import com.github.luben.zstd.Zstd;
import com.google.gson.FormattingStyle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.faststats.core.chart.Chart;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class SimpleMetrics implements Metrics {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private @Nullable ScheduledExecutorService executor = null;

    private final Set<Chart<?>> charts;
    private final Config config;
    private final @Token String token;
    private final URI url;
    private final boolean debug;

    @SuppressWarnings("PatternValidation")
    protected SimpleMetrics(SimpleMetrics.Factory factory, Path config) throws IOException, IllegalStateException {
        if (factory.token == null) throw new IllegalStateException("Token must be specified");

        this.charts = new CopyOnWriteArraySet<>(factory.charts);
        this.config = new Config(config);
        this.debug = factory.debug;
        this.token = factory.token;
        this.url = factory.url;
    }

    @VisibleForTesting
    protected SimpleMetrics(Config config, Set<Chart<?>> charts, @Token String token, URI url, boolean debug) {
        this.charts = new CopyOnWriteArraySet<>(charts);
        this.config = config;
        this.debug = debug;
        this.token = token;
        this.url = url;
    }

    protected void addChart(Chart<?> chart) throws IllegalArgumentException {
        if (!charts.add(chart)) throw new IllegalArgumentException("Chart already added: " + chart.getId());
    }

    @Async.Schedule
    @MustBeInvokedByOverriders
    protected void startSubmitting(int initialDelay, int period, TimeUnit unit) {
        if (!config.enabled()) {
            warn("Metrics disabled, not starting submission");
            return;
        }

        if (isSubmitting()) {
            warn("Metrics already submitting, not starting again");
            return;
        }

        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "metrics-submitter");
            thread.setDaemon(true);
            return thread;
        });

        info("Starting metrics submission");
        executor.scheduleAtFixedRate(this::submitData, initialDelay, period, unit);
    }

    protected boolean isSubmitting() {
        return executor != null && !executor.isShutdown();
    }

    protected void submitData() {
        try {
            var data = createData().toString();
            var compressed = Zstd.compress(data.getBytes(StandardCharsets.UTF_8), 6);
            var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
                    .header("Content-Encoding", "zstd")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", "Bearer " + getToken())
                    .header("User-Agent", "FastStats Metrics")
                    .timeout(Duration.ofSeconds(3))
                    .uri(url)
                    .build();

            info("Sending metrics to: " + url);
            info("Uncompressed data: " + data);
            info("Compressed size: " + compressed.length + " bytes");

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var statusCode = response.statusCode();
            var body = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                info("Metrics submitted with status code: " + statusCode + " (" + body + ")");
            } else if (statusCode >= 300 && statusCode < 400) {
                warn("Received redirect response from metrics server: " + statusCode + " (" + body + ")");
            } else if (statusCode >= 400 && statusCode < 500) {
                error("Submitted invalid request to metrics server: " + statusCode + " (" + body + ")", null);
            } else if (statusCode >= 500 && statusCode < 600) {
                error("Received server error response from metrics server: " + statusCode + " (" + body + ")", null);
            } else {
                warn("Received unexpected response from metrics server: " + statusCode + " (" + body + ")");
            }

        } catch (HttpConnectTimeoutException e) {
            error("Metrics submission timed out after 3 seconds: " + url, null);
        } catch (ConnectException e) {
            error("Failed to connect to metrics server: " + url, null);
        } catch (Exception e) {
            error("Failed to submit metrics", e);
        }
    }

    protected JsonObject createData() {
        var data = new JsonObject();
        var charts = new JsonObject();

        charts.addProperty("java_version", System.getProperty("java.version"));
        charts.addProperty("os_arch", System.getProperty("os.arch"));
        charts.addProperty("os_name", System.getProperty("os.name"));
        charts.addProperty("os_version", System.getProperty("os.version"));
        charts.addProperty("core_count", Runtime.getRuntime().availableProcessors());

        this.charts.forEach(chart -> {
            try {
                chart.getData().ifPresent(chartData -> charts.add(chart.getId(), chartData));
            } catch (Exception e) {
                error("Failed to build chart data: " + chart.getId(), e);
            }
        });

        data.addProperty("server_id", config.serverId().toString());
        data.add("data", charts);
        return data;
    }

    @Override
    public @Token String getToken() {
        return token;
    }

    @Override
    public Metrics.Config getConfig() {
        return config;
    }

    protected boolean isDebug() {
        return debug || config.debug();
    }

    protected abstract void error(String message, @Nullable Throwable throwable);

    protected abstract void warn(String message);

    protected abstract void info(String message);

    @Override
    public void shutdown() {
        info("Shutting down metrics submission");
        if (executor == null) return;
        executor.shutdown();
        executor = null;
    }

    public abstract static class Factory implements Metrics.Factory {
        private final Set<Chart<?>> charts = new HashSet<>(0);
        private URI url = URI.create("https://metrics.faststats.dev/v1/collect");
        private @Nullable String token;
        private boolean debug = false;

        @Override
        public Metrics.Factory addChart(Chart<?> chart) throws IllegalArgumentException {
            if (!charts.add(chart)) throw new IllegalArgumentException("Chart already added: " + chart.getId());
            return this;
        }

        @Override
        public Metrics.Factory debug(boolean enabled) {
            this.debug = enabled;
            return this;
        }

        @Override
        public Metrics.Factory token(@Token String token) throws IllegalArgumentException {
            if (!token.matches(Token.PATTERN)) {
                throw new IllegalArgumentException("Invalid token '" + token + "', must match '" + Token.PATTERN + "'");
            }
            this.token = token;
            return this;
        }

        @Override
        public Metrics.Factory url(URI url) {
            this.url = url;
            return this;
        }
    }

    protected static final class Config implements Metrics.Config {
        private final boolean debug;
        private final boolean enabled;
        private final UUID serverId;

        protected Config(Path file) throws IOException {
            var json = readOrEmpty(file);

            this.serverId = json.map(object -> UUID.fromString(object.get("serverId").getAsString())).orElseGet(UUID::randomUUID);
            this.enabled = json.map(object -> object.get("enabled").getAsBoolean()).orElse(true);
            this.debug = json.map(object -> object.get("debug").getAsBoolean()).orElse(false);

            if (json.isEmpty()) create(file, serverId);
        }

        @VisibleForTesting
        public Config(UUID serverId, boolean enabled, boolean debug) {
            this.serverId = serverId;
            this.enabled = enabled;
            this.debug = debug;
        }

        @Override
        public UUID serverId() {
            return serverId;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public boolean debug() {
            return debug;
        }

        private static Optional<JsonObject> readOrEmpty(Path file) throws IOException {
            if (Files.isRegularFile(file)) {
                return Optional.of(read(file));
            } else {
                return Optional.empty();
            }
        }

        private static void create(Path file, UUID serverId) throws IOException {
            Files.createDirectories(file.getParent());
            try (var out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW);
                 var writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                writer.setFormattingStyle(FormattingStyle.PRETTY);
                writer.beginObject();
                writer.name("serverId").value(serverId.toString());
                writer.name("enabled").value(true);
                writer.name("debug").value(false);
                writer.endObject();
            }
        }

        private static JsonObject read(Path file) throws IOException {
            try (var reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
