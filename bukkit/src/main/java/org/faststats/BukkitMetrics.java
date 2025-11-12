package org.faststats;

import com.google.gson.FormattingStyle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.plugin.Plugin;
import org.faststats.chart.Chart;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

@NullMarked
public class BukkitMetrics extends SimpleMetrics {
    private final Plugin plugin;
    private final String token;
    private final UUID serverId;
    private final boolean debug;
    private final boolean enabled;
    private final boolean onlineMode;

    @SuppressWarnings("deprecation")
    public BukkitMetrics(Plugin plugin, String token) throws IOException {
        var dataFolder = plugin.getServer().getPluginsFolder().toPath().resolve("faststats");
        var config = dataFolder.resolve("config.json");

        var json = readOrCreate(config);

        this.serverId = json.map(object -> UUID.fromString(object.get("serverId").getAsString())).orElseGet(UUID::randomUUID);
        this.enabled = json.map(object -> object.get("enabled").getAsBoolean()).orElse(true);
        this.debug = json.map(object -> object.get("debug").getAsBoolean()).orElse(false);

        this.plugin = plugin;
        this.onlineMode = checkOnlineMode();

        this.token = token;

        addChart(Chart.pie("online_mode", () -> String.valueOf(onlineMode)));
        addChart(Chart.pie("plugin_version", () -> plugin.getDescription().getVersion()));
        addChart(Chart.pie("server_type", () -> plugin.getServer().getName()));
        addChart(Chart.pie("minecraft_version", () -> plugin.getServer().getMinecraftVersion()));
        addChart(Chart.line("player_count", () -> plugin.getServer().getOnlinePlayers().size()));
        startSubmitting();
    }

    private static Optional<JsonObject> readOrCreate(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return Optional.of(read(path));
        } else {
            create(path);
            return Optional.empty();
        }
    }

    private static void create(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (var out = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW);
             var writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.setFormattingStyle(FormattingStyle.PRETTY);
            writer.beginObject();
            writer.name("serverId").value(UUID.randomUUID().toString());
            writer.name("enabled").value(true);
            writer.name("debug").value(false);
            writer.endObject();
        }
    }

    private static JsonObject read(Path path) throws IOException {
        try (var reader = new JsonReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
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

    private <T> Optional<T> tryOrEmpty(Supplier<@Nullable T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (NoSuchMethodError | Exception e) {
            return Optional.empty();
        }
    }

    @Override
    protected UUID getServerId() {
        return serverId;
    }

    @Override
    protected boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void error(String message, Throwable throwable) {
        if (debug) plugin.getLogger().log(Level.SEVERE, message, throwable);
    }

    @Override
    protected void debug(String message) {
        if (debug) plugin.getLogger().log(Level.INFO, message);
    }

    @Override
    public String getToken() {
        return token;
    }
}
