package com.punestartup.resume.service;

import com.punestartup.common.exception.InvalidParameterException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeFileValidatorTest {

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final ResumeFileValidator validator = new ResumeFileValidator(MAX_SIZE);

    @Test
    void acceptsTextResumeAndSanitizesFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "C:\\Users\\ada\\resume.txt",
                "text/plain", "Java and SQL".getBytes(StandardCharsets.UTF_8));

        ValidatedResumeFile validated = validator.validate(file);

        assertThat(validated.originalFilename()).isEqualTo("resume.txt");
        assertThat(validated.contentType()).isEqualTo("text/plain");
        assertThat(validated.extension()).isEqualTo("txt");
        assertThat(validated.content()).isEqualTo("Java and SQL".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void acceptsPdfAndDocxSignatures() throws IOException {
        ValidatedResumeFile pdf = validator.validate(new MockMultipartFile("file", "resume.pdf",
                "application/octet-stream",
                "%PDF-1.7\nresume".getBytes(StandardCharsets.US_ASCII)));
        ValidatedResumeFile docx = validator.validate(new MockMultipartFile("file", "resume.docx",
                "application/octet-stream", docxContent()));

        assertThat(pdf.contentType()).isEqualTo("application/pdf");
        assertThat(docx.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    void rejectsMismatchedContent() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume.pdf",
                "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("PDF");
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume.docx",
                "application/octet-stream", new byte[]{'P', 'K', 3, 4, 1})))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("DOCX");
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume.txt",
                "text/plain", new byte[]{'a', 0, 'b'})))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("TXT");
    }

    @Test
    void rejectsMissingUnsupportedEmptyAndOversizedFiles() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume.txt",
                "text/plain", new byte[0])))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume.exe",
                "application/octet-stream", new byte[]{1, 2, 3})))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("PDF, DOCX, or TXT");
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume",
                "text/plain", "text".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("extension");
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume.txt",
                "text/plain", new byte[0])))
                .isInstanceOf(InvalidParameterException.class);

        byte[] oversized = new byte[(int) MAX_SIZE + 1];
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume.txt",
                "text/plain", oversized)))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void rejectsInvalidFilenames() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", null,
                "text/plain", "text".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("filename");
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile("file", "resume\n.txt",
                "text/plain", "text".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("valid filename");
    }

    private byte[] docxContent() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
