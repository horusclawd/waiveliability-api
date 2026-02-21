package com.waiveliability.common.storage;

import com.waiveliability.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;

    /**
     * Uploads an object to S3 and returns the key.
     */
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        String bucket = s3Config.getS3().getBucket();

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(contentLength)
            .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        log.debug("Uploaded S3 object: bucket={}, key={}", bucket, key);
        return key;
    }

    /**
     * Downloads an object from S3 and returns its bytes.
     */
    public byte[] download(String key) throws IOException {
        String bucket = s3Config.getS3().getBucket();

        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest)) {
            log.debug("Downloaded S3 object: bucket={}, key={}", bucket, key);
            return response.readAllBytes();
        }
    }

    /**
     * Deletes an object from S3.
     */
    public void delete(String key) {
        String bucket = s3Config.getS3().getBucket();

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        s3Client.deleteObject(deleteRequest);
        log.debug("Deleted S3 object: bucket={}, key={}", bucket, key);
    }

    /**
     * Generates a pre-signed GET URL for the given key with the specified expiry.
     */
    public String generateSignedUrl(String key, Duration expiry) {
        String bucket = s3Config.getS3().getBucket();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .getObjectRequest(req -> req.bucket(bucket).key(key))
            .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.debug("Generated signed URL for S3 object: bucket={}, key={}", bucket, key);
        return url;
    }
}
