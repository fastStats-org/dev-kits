package org.faststats;

import com.google.gson.FormattingStyle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.plugin.Plugin;
import org.faststats.chart.Chart;
import org.jetbrains.annotations.TestOnly;
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
import java.util.concurrent.TimeUnit;
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
        setup();
    }

    @TestOnly
    public BukkitMetrics(Plugin plugin, String token, UUID serverId, boolean enabled, boolean debug) {
        this.serverId = serverId;
        this.enabled = enabled;
        this.debug = debug;

        this.plugin = plugin;
        this.onlineMode = checkOnlineMode();

        this.token = token;
        setup();
    }

    @SuppressWarnings("deprecation")
    protected void setup() {
        addChart(Chart.bool("online_mode", () -> onlineMode));
        addChart(Chart.string("plugin_version", () -> plugin.getDescription().getVersion()));
        addChart(Chart.string("server_type", () -> plugin.getServer().getName()));
        addChart(Chart.string("minecraft_version", () -> plugin.getServer().getMinecraftVersion()));
        addChart(Chart.number("player_count", () -> {
            var size = plugin.getServer().getOnlinePlayers().size();
            return size != 0 ? size : null;
        }));
        startSubmitting();
    }

    protected void startSubmitting() {
        startSubmitting(0, 30, TimeUnit.MINUTES);
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
        if (!debug) return;
        var msg = "[" + BukkitMetrics.class.getName() + "]: " + message;
        plugin.getLogger().log(Level.SEVERE, msg, throwable);
    }

    @Override
    protected void debug(String message) {
        if (!debug) return;
        var msg = "[" + BukkitMetrics.class.getName() + "]: " + message;
        plugin.getLogger().log(Level.INFO, msg);
    }

    @Override
    public String getToken() {
        return token;
    }

    private <T> Optional<T> tryOrEmpty(Supplier<@Nullable T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (NoSuchMethodError | Exception e) {
            error("Failed to call supplier", e);
            return Optional.empty();
        }
    }
}
