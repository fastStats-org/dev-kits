package org.faststats.metrics;

import com.google.gson.JsonObject;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public class BukkitMetrics extends Metrics {
    private final Plugin plugin;

    public BukkitMetrics(Plugin plugin, int projectId) {
        super(UUID.randomUUID() /* todo: faststats save file*/, true, projectId);
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected JsonObject createData() {
        var data = super.createData();
        data.addProperty("onlineMode", plugin.getServer().getOnlineMode() ? 1 : 0);
        data.addProperty("playerAmount", plugin.getServer().getOnlinePlayers().size());
        data.addProperty("pluginVersion", plugin.getDescription().getVersion());
        data.addProperty("serverName", plugin.getServer().getName());
        data.addProperty("serverVersion", plugin.getServer().getMinecraftVersion());
        return data;
    }
}
