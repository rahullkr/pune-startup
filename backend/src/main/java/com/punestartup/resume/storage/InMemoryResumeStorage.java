package com.punestartup.resume.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryResumeStorage implements ResumeStorage {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String key, InputStream content, long contentLength, String contentType, String sha256) {
        try {
            objects.put(key, content.readAllBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read resume content", ex);
        }
    }

    @Override
    public InputStream get(String key) {
        byte[] content = objects.get(key);
        if (content == null) {
            throw new IllegalStateException("Resume object not found: " + key);
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }

    @Override
    public String presignedGetUrl(String key, String filename) {
        return "memory://resumes/" + key;
    }

    public boolean exists(String key) {
        return objects.containsKey(key);
    }
}
