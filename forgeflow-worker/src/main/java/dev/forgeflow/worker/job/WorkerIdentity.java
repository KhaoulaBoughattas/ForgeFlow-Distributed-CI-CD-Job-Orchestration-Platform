package dev.forgeflow.worker.job;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/** A random, process-lifetime identifier this worker instance uses to tag jobs it claims. */
@Component
public class WorkerIdentity {

    private static final Logger log = LoggerFactory.getLogger(WorkerIdentity.class);

    private final String workerId;

    public WorkerIdentity() {
        byte[] randomBytes = new byte[4];
        new SecureRandom().nextBytes(randomBytes);
        this.workerId = "worker-" + HexFormat.of().formatHex(randomBytes);
    }

    @PostConstruct
    void logIdentity() {
        log.info("ForgeFlow worker starting up with identity {}", workerId);
    }

    public String id() {
        return workerId;
    }
}
