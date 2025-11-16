package org.faststats;

import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public class MockMetrics extends SimpleMetrics {
    private final UUID serverId;
    private final String token;
    
    public MockMetrics(UUID serverId, String token) {
        this.serverId = serverId;
        this.token = token;
    }    
    @Override
    protected void error(String message, Throwable throwable) {
        System.err.println(message);
        throwable.printStackTrace(System.err);
    }

    @Override
    protected void info(String message) {
        System.out.println(message);
    }

    @Override
    public JsonObject createData() {
        return super.createData();
    }

    @Override
    protected UUID getServerId() {
        return serverId;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    public String getToken() {
        return token;
    }
}
