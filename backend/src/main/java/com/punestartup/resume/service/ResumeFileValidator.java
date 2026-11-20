package com.punestartup.resume.service;

import com.punestartup.common.exception.InvalidParameterException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ResumeFileValidator {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String TEXT_CONTENT_TYPE = "text/plain";

    private final long maxSizeBytes;

    public ResumeFileValidator(@Value("${app.resume.max-size-bytes:5242880}") long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public ValidatedResumeFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw InvalidParameterException.of("file", "must not be empty");
        }
        if (file.getSize() > maxSizeBytes) {
            throw InvalidParameterException.of("file", "must be at most " + maxSizeBytes + " bytes");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String extension = extensionOf(filename);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw InvalidParameterException.of("file", "could not be read");
        }
        if (content.length == 0) {
            throw InvalidParameterException.of("file", "must not be empty");
        }
        if (content.length > maxSizeBytes) {
            throw InvalidParameterException.of("file", "must be at most " + maxSizeBytes + " bytes");
        }

        return switch (extension) {
            case "pdf" -> validatePdf(content, filename);
            case "docx" -> validateDocx(content, filename);
            case "txt" -> validateText(content, filename);
            default -> throw InvalidParameterException.of("file",
                    "must be a PDF, DOCX, or TXT file");
        };
    }

    private ValidatedResumeFile validatePdf(byte[] content, String filename) {
        int searchLimit = Math.min(content.length, 1024);
        byte[] header = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        if (!contains(content, header, searchLimit)) {
            throw InvalidParameterException.of("file", "content does not match the PDF extension");
        }
        return new ValidatedResumeFile(content, filename, PDF_CONTENT_TYPE, "pdf");
    }

    private ValidatedResumeFile validateDocx(byte[] content, String filename) {
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("[Content_Types].xml".equals(entry.getName())) {
                    hasContentTypes = true;
                } else if ("word/document.xml".equals(entry.getName())) {
                    hasDocument = true;
                }
                if (hasContentTypes && hasDocument) {
                    break;
                }
            }
        } catch (IOException ex) {
            throw InvalidParameterException.of("file", "content does not match the DOCX extension");
        }
        if (!hasContentTypes || !hasDocument) {
            throw InvalidParameterException.of("file", "content does not match the DOCX extension");
        }
        return new ValidatedResumeFile(content, filename, DOCX_CONTENT_TYPE, "docx");
    }

    private ValidatedResumeFile validateText(byte[] content, String filename) {
        int controlCharacters = 0;
        for (byte value : content) {
            int unsigned = value & 0xff;
            if (unsigned == 0) {
                throw InvalidParameterException.of("file", "content does not match the TXT extension");
            }
            if (unsigned < 0x20 && unsigned != '\n' && unsigned != '\r' && unsigned != '\t') {
                controlCharacters++;
            }
        }
        if (controlCharacters > Math.max(1, content.length / 100)) {
            throw InvalidParameterException.of("file", "content does not match the TXT extension");
        }
        return new ValidatedResumeFile(content, filename, TEXT_CONTENT_TYPE, "txt");
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw InvalidParameterException.of("file", "must have a filename");
        }
        String filename = originalFilename.replace('\\', '/');
        int slash = filename.lastIndexOf('/');
        if (slash >= 0) {
            filename = filename.substring(slash + 1);
        }
        filename = filename.trim();
        if (filename.isBlank() || filename.indexOf('\r') >= 0 || filename.indexOf('\n') >= 0) {
            throw InvalidParameterException.of("file", "must have a valid filename");
        }
        if (filename.length() > 255) {
            throw InvalidParameterException.of("file", "filename must be at most 255 characters");
        }
        return filename;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw InvalidParameterException.of("file", "must have a .pdf, .docx, or .txt extension");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean contains(byte[] content, byte[] needle, int limit) {
        if (content.length < needle.length) {
            return false;
        }
        int max = Math.min(limit, content.length - needle.length);
        for (int i = 0; i <= max; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (content[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }
}
