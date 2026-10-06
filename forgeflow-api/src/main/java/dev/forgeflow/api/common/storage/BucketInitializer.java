package dev.forgeflow.api.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Ensures the artifact bucket exists on startup. Failure here is intentionally non-fatal
 * (logged as a warning) so that a slow-starting MinIO container doesn't crash the whole API
 * on first boot in docker-compose.
 */
@Component
public class BucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BucketInitializer.class);

    private final S3Client s3Client;
    private final String bucketName;

    public BucketInitializer(S3Client s3Client, @Value("${forgeflow.storage.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("Artifact bucket '{}' already exists", bucketName);
        } catch (S3Exception ex) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Created artifact bucket '{}'", bucketName);
            } catch (Exception createEx) {
                log.warn("Could not create artifact bucket '{}' on startup: {}", bucketName, createEx.getMessage());
            }
        } catch (Exception ex) {
            log.warn("Could not verify artifact bucket '{}' on startup: {}", bucketName, ex.getMessage());
        }
    }
}
