package org.faststats;

import com.github.luben.zstd.Zstd;
import com.google.gson.JsonObject;
import org.faststats.chart.Chart;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class SimpleMetrics implements Metrics {
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final Set<Chart<?>> charts = new HashSet<>();
    private @Nullable ScheduledExecutorService executor = null;

    protected void startSubmitting(int initialDelay, int period, TimeUnit unit) {
        if (!isEnabled()) {
            debug("Metrics disabled, not starting submission");
            return;
        }

        if (isSubmitting()) {
            debug("Metrics already submitting, not starting again");
            return;
        }

        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "metrics-submitter");
            thread.setDaemon(true);
            return thread;
        });

        debug("Starting metrics submission");
        executor.scheduleAtFixedRate(this::submitData, initialDelay, period, unit);
    }

    protected boolean isSubmitting() {
        return executor != null && !executor.isShutdown();
    }

    @Override
    public void addChart(Chart<?> chart) {
        charts.add(chart);
    }

    protected void submitData() {
        try {
            var bytes = createData().toString().getBytes(StandardCharsets.UTF_8);
            var compressed = Zstd.compress(bytes, 6);
            var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
                    .header("Content-Encoding", "zstd")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", "Bearer " + getToken())
                    .header("User-Agent", "fastStats Metrics")
                    .timeout(Duration.ofSeconds(3))
                    .uri(URI.create(getURL()))
                    .build();
            var send = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            debug("Metrics submitted with status code: " + send.statusCode());
        } catch (IOException | InterruptedException e) {
            // todo: shorten connection errors
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

        data.addProperty("server_id", getServerId().toString());
        data.add("data", charts);
        return data;
    }

    protected String getURL() {
        return "https://api.faststats.org/v1/metrics";
    }

    protected abstract UUID getServerId();

    protected abstract boolean isEnabled();

    protected abstract void error(String message, Throwable throwable);

    protected abstract void debug(String message);

    public void shutdown() {
        debug("Shutting down metrics");
        if (executor == null) return;
        executor.shutdown();
        executor = null;
    }
}
