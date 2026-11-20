package com.punestartup.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.punestartup.auth.repository.AuthSessionRepository;
import com.punestartup.auth.repository.UserRepository;
import com.punestartup.auth.repository.UserRoleRepository;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.CandidateSkillRepository;
import com.punestartup.resume.entity.CandidateResume;
import com.punestartup.resume.entity.ResumeDeletedReason;
import com.punestartup.resume.entity.ResumeProcessingStatus;
import com.punestartup.resume.repository.CandidateResumeRepository;
import com.punestartup.resume.service.ResumeRetentionService;
import com.punestartup.resume.storage.InMemoryResumeStorage;
import com.punestartup.resume.storage.ResumeStorage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.api-key=test-admin-key",
                "app.resume.cleanup-enabled=true",
                "app.resume.cleanup-initial-delay=PT1H"
        })
@Testcontainers
class CandidateResumeApiIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String PROFILE_URL = "/api/v1/candidates/me";
    private static final String RESUMES_URL = "/api/v1/candidates/me/resumes";
    private static final String RESUME_TEXT = "Java Spring Boot engineer with SQL experience. "
            + "Private resume text that must never appear in an API response.";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private AuthSessionRepository sessionRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CandidateProfileRepository profileRepository;
    @Autowired
    private CandidateSkillRepository candidateSkillRepository;
    @Autowired
    private CandidateResumeRepository resumeRepository;
    @Autowired
    private ResumeStorage resumeStorage;
    @Autowired
    private ResumeRetentionService retentionService;

    private String token;

    @BeforeEach
    void cleanAndRegister() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        resumeRepository.deleteAll();
        sessionRepository.deleteAll();
        candidateSkillRepository.deleteAll();
        profileRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        token = register("resume@example.com", "Resume Candidate");
    }

    @Test
    void uploadsParsesListsGetsAndDeletesResumePrivately() {
        assertThat(upload("resume.txt", RESUME_TEXT.getBytes(StandardCharsets.UTF_8), null, "true", true)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(upload("resume.txt", RESUME_TEXT.getBytes(StandardCharsets.UTF_8), token, "false", true)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<JsonNode> created =
                upload("resume.exe", new byte[]{1, 2, 3}, token, "true", true);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<JsonNode> response =
                upload("resume.txt", RESUME_TEXT.getBytes(StandardCharsets.UTF_8), token, "true", true);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = response.getBody();
        assertThat(body.path("originalFilename").asText()).isEqualTo("resume.txt");
        assertThat(body.path("contentType").asText()).isEqualTo("text/plain");
        assertThat(body.path("primary").asBoolean()).isTrue();
        assertThat(body.path("consentVersion").asText()).isEqualTo("v1");
        assertThat(body.path("retentionExpiresAt").asText()).isNotBlank();
        assertThat(body.path("downloadUrl").asText()).startsWith("memory://resumes/");
        assertThat(body.toString()).doesNotContain("Private resume text");

        UUID resumeId = UUID.fromString(body.path("id").asText());
        JsonNode completed = awaitCompleted(resumeId);
        assertThat(completed.path("processingStatus").asText()).isEqualTo("COMPLETED");
        assertThat(completed.path("parsedSkillCount").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(completed.path("processedAt").asText()).isNotBlank();
        assertThat(completed.path("downloadUrl").asText()).startsWith("memory://resumes/");
        assertThat(completed.toString()).doesNotContain("Private resume text");

        Map<String, String> parsedSkills = parsedSkillSources(resumeId);
        assertThat(parsedSkills).containsEntry("Java", "RESUME_PARSED");
        assertThat(parsedSkills).containsEntry("Spring Boot", "RESUME_PARSED");
        assertThat(parsedSkills).containsEntry("SQL", "RESUME_PARSED");

        JsonNode list = get(RESUMES_URL, token).getBody();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).path("id").asText()).isEqualTo(resumeId.toString());
        assertThat(get(RESUMES_URL + "/" + resumeId, token).getBody().path("id").asText())
                .isEqualTo(resumeId.toString());

        ResponseEntity<JsonNode> deleted = delete(RESUMES_URL + "/" + resumeId, token);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(RESUMES_URL, token).getBody()).isEmpty();
        assertThat(get(RESUMES_URL + "/" + resumeId, token).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        CandidateResume stored = resumeRepository.findById(resumeId).orElseThrow();
        assertThat(stored.getDeletedAt()).isNotNull();
        assertThat(stored.getDeletedReason()).isEqualTo(ResumeDeletedReason.CANDIDATE_REQUEST);
        assertThat(((InMemoryResumeStorage) resumeStorage).exists(stored.getStorageKey())).isFalse();
    }

    @Test
    void replacesPrimaryResumeAndPromotesRemainingResumeOnDelete() {
        UUID first = UUID.fromString(upload("first.txt", "Java".getBytes(StandardCharsets.UTF_8),
                token, "true", true).getBody().path("id").asText());
        awaitCompleted(first);

        UUID second = UUID.fromString(upload("second.txt", "Spring Boot".getBytes(StandardCharsets.UTF_8),
                token, "true", true).getBody().path("id").asText());
        awaitCompleted(second);

        JsonNode list = get(RESUMES_URL, token).getBody();
        assertThat(list).hasSize(2);
        assertThat(primaryId(list)).isEqualTo(second);

        assertThat(delete(RESUMES_URL + "/" + second, token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode afterDelete = get(RESUMES_URL, token).getBody();
        assertThat(afterDelete).hasSize(1);
        assertThat(primaryId(afterDelete)).isEqualTo(first);
    }

    @Test
    void hidesExpiredResumeAndDeletesItDuringRetentionCleanup() {
        UUID resumeId = UUID.fromString(upload("expired.txt", "Python".getBytes(StandardCharsets.UTF_8),
                token, "true", true).getBody().path("id").asText());
        awaitCompleted(resumeId);

        CandidateResume resume = resumeRepository.findById(resumeId).orElseThrow();
        resume.setRetentionExpiresAt(Instant.now().minusSeconds(1));
        resumeRepository.saveAndFlush(resume);

        assertThat(get(RESUMES_URL, token).getBody()).isEmpty();
        assertThat(get(RESUMES_URL + "/" + resumeId, token).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        retentionService.cleanupExpired();

        CandidateResume cleaned = resumeRepository.findById(resumeId).orElseThrow();
        assertThat(cleaned.getDeletedAt()).isNotNull();
        assertThat(cleaned.getDeletedReason()).isEqualTo(ResumeDeletedReason.RETENTION_EXPIRED);
        assertThat(((InMemoryResumeStorage) resumeStorage).exists(cleaned.getStorageKey())).isFalse();
    }

    @Test
    void deactivatingAccountDeletesResumeObjectsAndRows() {
        UUID resumeId = UUID.fromString(upload("account.txt", "React".getBytes(StandardCharsets.UTF_8),
                token, "true", true).getBody().path("id").asText());
        awaitCompleted(resumeId);

        assertThat(delete(PROFILE_URL, token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        CandidateResume resume = resumeRepository.findById(resumeId).orElseThrow();
        assertThat(resume.getDeletedAt()).isNotNull();
        assertThat(resume.getDeletedReason()).isEqualTo(ResumeDeletedReason.ACCOUNT_DEACTIVATED);
        assertThat(((InMemoryResumeStorage) resumeStorage).exists(resume.getStorageKey())).isFalse();
        assertThat(get(RESUMES_URL, token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enforcesOwnershipAndValidatesContentTypeAndSize() {
        UUID resumeId = UUID.fromString(upload("owned.txt", "Java".getBytes(StandardCharsets.UTF_8),
                token, "true", true).getBody().path("id").asText());
        awaitCompleted(resumeId);
        String otherToken = register("other@example.com", "Other Candidate");

        assertThat(get(RESUMES_URL + "/" + resumeId, otherToken).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(delete(RESUMES_URL + "/" + resumeId, otherToken).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(upload("fake.pdf", "not a pdf".getBytes(StandardCharsets.UTF_8),
                token, "true", true).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(upload("fake.docx", new byte[]{'P', 'K', 3, 4, 1, 2, 3},
                token, "true", true).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(upload("empty.txt", new byte[0], token, "true", true).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(upload("large.txt", new byte[5 * 1024 * 1024 + 1], token, "true", true)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(upload("resume.txt", "text".getBytes(StandardCharsets.UTF_8),
                token, "true", false).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private JsonNode awaitCompleted(UUID resumeId) {
        for (int attempt = 0; attempt < 60; attempt++) {
            JsonNode body = get(RESUMES_URL + "/" + resumeId, token).getBody();
            String status = body.path("processingStatus").asText();
            if (status.equals(ResumeProcessingStatus.COMPLETED.name())
                    || status.equals(ResumeProcessingStatus.FAILED.name())) {
                return body;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }
        throw new AssertionError("Resume " + resumeId + " did not finish processing");
    }

    private Map<String, String> parsedSkillSources(UUID resumeId) {
        CandidateResume resume = resumeRepository.findById(resumeId).orElseThrow();
        Map<String, String> result = new LinkedHashMap<>();
        candidateSkillRepository.findByCandidateProfile_Id(resume.getCandidateProfile().getId())
                .forEach(skill -> result.put(skill.getSkill().getName(), skill.getSource().name()));
        return result;
    }

    private UUID primaryId(JsonNode list) {
        for (JsonNode resume : list) {
            if (resume.path("primary").asBoolean()) {
                return UUID.fromString(resume.path("id").asText());
            }
        }
        return null;
    }

    private ResponseEntity<JsonNode> upload(String filename, byte[] content, String bearerToken,
                                            String consent, boolean primary) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(content, filename));
        body.add("consent", consent);
        body.add("consentVersion", "v1");
        body.add("primary", Boolean.toString(primary));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return restTemplate.exchange(RESUMES_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
    }

    private String register(String email, String fullName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", "correct-horse-1");
        body.put("fullName", fullName);
        ResponseEntity<JsonNode> response = post(REGISTER_URL, body, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().path("token").asText();
    }

    private ResponseEntity<JsonNode> get(String path, String bearerToken) {
        return restTemplate.exchange(path, HttpMethod.GET, entity(null, bearerToken), JsonNode.class);
    }

    private ResponseEntity<JsonNode> post(String path, Object body, String bearerToken) {
        return restTemplate.exchange(path, HttpMethod.POST, entity(body, bearerToken), JsonNode.class);
    }

    private ResponseEntity<JsonNode> delete(String path, String bearerToken) {
        return restTemplate.exchange(path, HttpMethod.DELETE, entity(null, bearerToken), JsonNode.class);
    }

    private HttpEntity<Object> entity(Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return new HttpEntity<>(body, headers);
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
