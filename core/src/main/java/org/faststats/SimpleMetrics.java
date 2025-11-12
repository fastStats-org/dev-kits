package org.faststats;

import com.github.luben.zstd.Zstd;
import com.google.gson.JsonObject;
import org.faststats.chart.Chart;

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
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final Set<Chart<?>> charts = new HashSet<>();

    private final ScheduledExecutorService executor;

    public SimpleMetrics() {
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "metrics-submitter");
            thread.setDaemon(true);
            return thread;
        });
    }

    protected void startSubmitting() {
        if (isEnabled()) executor.scheduleAtFixedRate(this::submitData, 0, 30, TimeUnit.MINUTES);
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
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            error("Failed to submit metrics", e);
        }
    }

    protected JsonObject createData() {
        var data = new JsonObject();
        data.addProperty("serverIdentifier", getServerId().toString());
        data.addProperty("token", getToken());

        data.addProperty("javaVersion", System.getProperty("java.version"));
        data.addProperty("locale", System.getProperty("user.language"));
        data.addProperty("osArch", System.getProperty("os.arch"));
        data.addProperty("osName", System.getProperty("os.name"));
        data.addProperty("osVersion", System.getProperty("os.version"));
        data.addProperty("processors", Runtime.getRuntime().availableProcessors());

        var charts = new JsonObject();
        this.charts.forEach(chart -> {
            try {
                chart.getData().ifPresent(chartData -> charts.add(chart.getId(), chartData));
            } catch (Exception e) {
                error("Failed to build chart data: " + chart.getId(), e);
            }
        });
        if (!charts.isEmpty()) data.add("charts", charts);
        return data;
    }

    protected String getURL() {
        return "https://api.faststats.org/v1/metrics";
    }

    protected abstract UUID getServerId();

    protected abstract boolean isEnabled();

    protected abstract void error(String message, Throwable throwable);

    protected abstract void info(String message);

    public void shutdown() {
        executor.shutdown();
    }
}
