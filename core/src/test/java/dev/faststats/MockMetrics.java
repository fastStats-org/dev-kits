package dev.faststats;

import com.google.gson.JsonObject;
import dev.faststats.core.SimpleMetrics;
import dev.faststats.core.Token;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@NullMarked
public class MockMetrics extends SimpleMetrics {
    public MockMetrics(UUID serverId, @Token String token, boolean debug) {
        super(new SimpleMetrics.Config(serverId, true, debug), Set.of(), token, URI.create("http://localhost:5000"), debug);
    }

    @Override
    protected void error(String message, @Nullable Throwable throwable) {
        if (!isDebug()) return;
        System.err.println(message);
        if (throwable != null) throwable.printStackTrace(System.err);
    }

    @Override
    protected void warn(String message) {
        if (isDebug()) System.out.println(message);
    }

    @Override
    protected void info(String message) {
        if (isDebug()) System.out.println(message);
    }

    @Override
    public JsonObject createData() {
        return super.createData();
    }

    @Override
    protected void appendDefaultData(JsonObject charts) {
    }
}
