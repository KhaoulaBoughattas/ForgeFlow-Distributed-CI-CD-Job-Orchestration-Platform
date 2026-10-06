package dev.forgeflow.worker.job;

/** Any unrecoverable failure while preparing or running a job (clone failure, docker not reachable, etc). */
public class JobExecutionException extends RuntimeException {
    public JobExecutionException(String message) {
        super(message);
    }

    public JobExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
