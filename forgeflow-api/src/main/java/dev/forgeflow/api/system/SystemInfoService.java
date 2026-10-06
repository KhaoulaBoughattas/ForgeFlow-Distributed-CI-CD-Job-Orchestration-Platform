package dev.forgeflow.api.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Tracks the application's own start time so uptime can be reported without an external clock. */
@Service
public class SystemInfoService {

    private final String version;
    private final String environment;
    private volatile Instant startedAt = Instant.now();

    public SystemInfoService(
            @Value("${forgeflow.version:0.1.0-SNAPSHOT}") String version,
            @Value("${forgeflow.environment:local}") String environment) {
        this.version = version;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        this.startedAt = Instant.now();
    }

    public SystemInfoResponse currentInfo() {
        long uptimeSeconds = Instant.now().getEpochSecond() - startedAt.getEpochSecond();
        return new SystemInfoResponse(version, environment, Math.max(uptimeSeconds, 0));
    }
}
