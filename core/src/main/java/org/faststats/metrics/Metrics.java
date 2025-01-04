package org.faststats.metrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.faststats.metrics.chart.Chart;
import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

@NullMarked
public abstract class Metrics {
    private static final String URL = "https://api.faststats.org/v1/";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Set<Chart<?>> charts = new HashSet<>();

    private final ScheduledExecutorService executor;

    private final UUID consumerId;
    private final int projectId;

    public Metrics(UUID consumerId, boolean enabled, int projectId) {
        this.consumerId = consumerId;
        this.projectId = projectId;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "metrics-submitter");
            thread.setDaemon(true);
            return thread;
        });
        if (enabled) executor.scheduleAtFixedRate(this::submitData, 1, 30, TimeUnit.MINUTES);
    }

    public void addChart(Chart<?> chart) {
        charts.add(chart);
    }

    private void submitData() {
        try {
            var data = compressData(createData());
            var request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .header("Content-Type", "application/json")
                    .header("Content-Encoding", "gzip")
                    .header("User-Agent", "fastStats Metrics")
                    .expectContinue(false)
                    .build();
            httpClient.send(request, null);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] compressData(JsonObject data) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(output)) {
            gzip.write(data.toString().getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        }
    }

    protected JsonObject createData() {
        var data = new JsonObject();
        data.addProperty("consumerId", consumerId.toString());
        data.addProperty("projectId", projectId);

        data.addProperty("javaVersion", System.getProperty("java.version"));
        data.addProperty("osName", System.getProperty("os.name"));
        data.addProperty("osArch", System.getProperty("os.arch"));
        data.addProperty("osVersion", System.getProperty("os.version"));
        data.addProperty("coreCount", Runtime.getRuntime().availableProcessors());

        var charts = new JsonArray(this.charts.size());
        this.charts.forEach(chart -> {
            try {
                charts.add(chart.getData());
            } catch (Exception e) {
                e.printStackTrace(); // todo: proper logging
            }
        });
        data.add("charts", charts);
        return data;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
