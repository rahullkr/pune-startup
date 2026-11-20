package com.punestartup.resume.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.r2")
public class R2StorageProperties {

    private boolean enabled = true;
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String secretAccessKey;
    private String region = "auto";
    private Duration presignExpiry = Duration.ofMinutes(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Duration getPresignExpiry() {
        return presignExpiry;
    }

    public void setPresignExpiry(Duration presignExpiry) {
        this.presignExpiry = presignExpiry;
    }
}
