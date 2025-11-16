package dev.faststats;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BukkitMetricsTest {
    @Test
    public void testCreateData() {
        var mock = new MockMetrics(UUID.randomUUID(), "bba4a14eac38779007a6fda4814381", true);
        var data = mock.createData();
        var bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        var compressed = Zstd.compress(bytes, 6);
        mock.info(new String(compressed, StandardCharsets.UTF_8) + " (" + compressed.length + " bytes)");
        mock.info(new String(bytes, StandardCharsets.UTF_8) + " (" + bytes.length + " bytes)");
    }
}
