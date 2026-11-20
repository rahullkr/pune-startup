package com.punestartup.resume.storage;

import java.io.InputStream;

public interface ResumeStorage {

    void put(String key, InputStream content, long contentLength, String contentType, String sha256);

    InputStream get(String key);

    void delete(String key);

    String presignedGetUrl(String key, String filename);
}
