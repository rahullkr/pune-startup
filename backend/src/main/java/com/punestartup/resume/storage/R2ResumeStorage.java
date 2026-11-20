package com.punestartup.resume.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class R2ResumeStorage implements ResumeStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final R2StorageProperties properties;

    public R2ResumeStorage(S3Client s3Client, S3Presigner presigner, R2StorageProperties properties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.properties = properties;
    }

    @Override
    public void put(String key, InputStream content, long contentLength, String contentType, String sha256) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .metadata(Map.of("sha256", sha256))
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));
    }

    @Override
    public InputStream get(String key) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
    }

    @Override
    public String presignedGetUrl(String key, String filename) {
        Duration expiry = properties.getPresignExpiry() == null
                ? Duration.ofMinutes(5)
                : properties.getPresignExpiry();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .responseContentDisposition(contentDisposition(filename))
                        .build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    private String contentDisposition(String filename) {
        String safe = filename == null ? "resume" : filename.replaceAll("[^A-Za-z0-9._ -]", "_");
        return "attachment; filename=\"" + safe + "\"";
    }
}
