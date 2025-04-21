package org.faststats;

import com.google.gson.JsonObject;
import org.faststats.chart.Chart;
import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayOutputStream;
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
import java.util.zip.GZIPOutputStream;

@NullMarked
public abstract class Metrics {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final Set<Chart<?>> charts = new HashSet<>();

    private final ScheduledExecutorService executor;

    private final UUID consumerId;
    private final boolean enabled;
    private final int projectId;

    public Metrics(UUID consumerId, boolean enabled, int projectId) {
        this.consumerId = consumerId;
        this.enabled = enabled;
        this.projectId = projectId;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "metrics-submitter");
            thread.setDaemon(true);
            return thread;
        });
    }

    protected void startSubmitting() {
        if (enabled) executor.scheduleAtFixedRate(this::submitData, 0, 30, TimeUnit.MINUTES);
    }

    public void addChart(Chart<?> chart) {
        charts.add(chart);
    }

    protected void submitData() {
        try {
            var data = compressData(createData());
            var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .header("Content-Encoding", "gzip")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "fastStats Metrics")
                    .timeout(Duration.ofSeconds(1))
                    .uri(URI.create(getURL()))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            error("Failed to submit metrics", e);
        }
    }

    private byte[] compressData(JsonObject data) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(output)) {
            gzip.write(data.toString().getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }

    private JsonObject createData() {
        var data = new JsonObject();
        data.addProperty("consumerId", consumerId.toString());
        data.addProperty("projectId", projectId);

        data.addProperty("javaVersion", System.getProperty("java.version"));
        data.addProperty("locale", System.getProperty("user.language"));
        data.addProperty("osArch", System.getProperty("os.arch"));
        data.addProperty("osName", System.getProperty("os.name"));
        data.addProperty("osVersion", System.getProperty("os.version"));
        data.addProperty("processors", Runtime.getRuntime().availableProcessors());

        var charts = new JsonObject();
        this.charts.forEach(chart -> {
            try {
                charts.add(chart.getId(), chart.getData());
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

    protected abstract void error(String message, Throwable throwable);

    public void shutdown() {
        executor.shutdown();
    }
}
