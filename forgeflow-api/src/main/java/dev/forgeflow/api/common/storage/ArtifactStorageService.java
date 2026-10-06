package dev.forgeflow.api.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

/** Produces short-lived, presigned download URLs for pipeline log archives stored in MinIO/S3. */
@Service
public class ArtifactStorageService {

    private final S3Presigner s3Presigner;
    private final String bucketName;

    public ArtifactStorageService(S3Presigner s3Presigner, @Value("${forgeflow.storage.bucket}") String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public URL presignDownloadUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }
}
