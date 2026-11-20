package com.punestartup.resume.storage;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true", matchIfMissing = true)
    public S3Client r2S3Client(R2StorageProperties properties) {
        requireConfigured(properties);
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true", matchIfMissing = true)
    public S3Presigner r2S3Presigner(R2StorageProperties properties) {
        requireConfigured(properties);
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "false")
    public ResumeStorage inMemoryResumeStorage() {
        return new InMemoryResumeStorage();
    }

    private StaticCredentialsProvider credentials(R2StorageProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey()));
    }

    private void requireConfigured(R2StorageProperties properties) {
        if (isBlank(properties.getEndpoint())
                || isBlank(properties.getBucket())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getSecretAccessKey())) {
            throw new IllegalStateException(
                    "R2 storage is enabled but R2_ENDPOINT, R2_BUCKET_NAME, R2_ACCESS_KEY_ID, "
                            + "and R2_SECRET_ACCESS_KEY are not all configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
