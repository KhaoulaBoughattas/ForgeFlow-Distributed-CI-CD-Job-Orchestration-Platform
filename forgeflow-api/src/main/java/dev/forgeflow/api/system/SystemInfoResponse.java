package dev.forgeflow.api.system;

/** Basic build/runtime information exposed at GET /api/v1/system/info. */
public record SystemInfoResponse(String version, String environment, long uptimeSeconds) {
}
