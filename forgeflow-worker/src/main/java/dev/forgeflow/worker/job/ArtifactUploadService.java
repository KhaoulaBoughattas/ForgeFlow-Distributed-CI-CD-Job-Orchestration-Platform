package dev.forgeflow.worker.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Uploads a completed job's full log to the shared artifact bucket under a per-pipeline key. */
@Service
public class ArtifactUploadService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactUploadService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public ArtifactUploadService(S3Client s3Client, @Value("${forgeflow.storage.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /** Uploads the full log and returns the object key it was stored under, or null on failure. */
    public String uploadLog(UUID pipelineId, String fullLog) {
        String objectKey = "pipelines/" + pipelineId + "/log.txt";
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key(objectKey).contentType("text/plain").build(),
                    RequestBody.fromBytes(fullLog.getBytes(StandardCharsets.UTF_8)));
            return objectKey;
        } catch (Exception ex) {
            // A failed log upload should not fail the whole job -- the pipeline's status and
            // logTail are still reported; only the full-log download link will be unavailable.
            log.error("Failed to upload log for pipeline {} to bucket {}", pipelineId, bucketName, ex);
            return null;
        }
    }
}
