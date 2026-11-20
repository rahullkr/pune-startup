package com.punestartup.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.punestartup.auth.repository.AuthSessionRepository;
import com.punestartup.auth.repository.UserRepository;
import com.punestartup.auth.repository.UserRoleRepository;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.CandidateSkillRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.admin.api-key=test-admin-key")
@Testcontainers
class CandidateAuthApiIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String PASSWORD_URL = "/api/v1/auth/password";
    private static final String PROFILE_URL = "/api/v1/candidates/me";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthSessionRepository sessionRepository;

    @Autowired
    private CandidateSkillRepository candidateSkillRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        sessionRepository.deleteAll();
        candidateSkillRepository.deleteAll();
        candidateProfileRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registersLogsInAndManagesProfile() {
        String email = "ada@example.com";
        ResponseEntity<JsonNode> registered = post(REGISTER_URL, registerBody(email, "correct-horse-1", "Ada Lovelace"), null);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode auth = registered.getBody();
        String token = auth.path("token").asText();
        assertThat(token).startsWith("psa_");
        assertThat(auth.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(Instant.parse(auth.path("expiresAt").asText())).isAfter(Instant.now());
        assertThat(auth.path("candidate").path("email").asText()).isEqualTo(email);
        assertThat(auth.path("candidate").path("fullName").asText()).isEqualTo("Ada Lovelace");
        assertThat(auth.path("candidate").path("skills")).isEmpty();

        assertThat(post(REGISTER_URL, registerBody(email, "correct-horse-1", "Ada Again"), null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(get(PROFILE_URL, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get(PROFILE_URL, token).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("fullName", "Ada Byron");
        update.put("headline", "Backend engineer");
        update.put("summary", "Builds reliable services.");
        update.put("totalExperienceMonths", 48);
        update.put("preferredCity", "Pune");
        update.put("preferredWorkMode", "HYBRID");
        update.put("skills", List.of(
                Map.of("name", "Java", "proficiency", "ADVANCED", "yearsExperience", 4.0),
                Map.of("name", "Spring Boot", "proficiency", "INTERMEDIATE", "yearsExperience", 3.0)));
        ResponseEntity<JsonNode> updated = patch(PROFILE_URL, update, token);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode profile = updated.getBody();
        assertThat(profile.path("fullName").asText()).isEqualTo("Ada Byron");
        assertThat(profile.path("preferredWorkMode").asText()).isEqualTo("HYBRID");
        assertThat(profile.path("totalExperienceMonths").asInt()).isEqualTo(48);
        assertThat(profile.path("skills")).hasSize(2);
        assertThat(profile.path("skills").get(0).path("skillName").asText()).isEqualTo("Java");
        assertThat(profile.path("skills").get(0).path("source").asText()).isEqualTo("SELF_DECLARED");

        ResponseEntity<JsonNode> cleared = patch(PROFILE_URL, Map.of("skills", List.of()), token);
        assertThat(cleared.getBody().path("skills")).isEmpty();

        assertThat(patch(PROFILE_URL, Map.of("preferredWorkMode", "UNKNOWN"), token).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(patch(PROFILE_URL, Map.of("skills", List.of(Map.of("name", "COBOL 9000"))), token)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(post(LOGIN_URL, loginBody(email, "wrong-password"), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        ResponseEntity<JsonNode> loggedIn = post(LOGIN_URL, loginBody(email, "correct-horse-1"), null);
        assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondToken = loggedIn.getBody().path("token").asText();
        assertThat(secondToken).isNotEqualTo(token);

        assertThat(post(LOGOUT_URL, null, token).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(PROFILE_URL, token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get(PROFILE_URL, secondToken).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(delete(PROFILE_URL, secondToken).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(PROFILE_URL, secondToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ResponseEntity<JsonNode> inactive = post(LOGIN_URL, loginBody(email, "correct-horse-1"), null);
        assertThat(inactive.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(inactive.getBody().path("error").asText()).isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    void changesPasswordAndRevokesOtherSessions() {
        String email = "grace@example.com";
        String firstToken = post(REGISTER_URL, registerBody(email, "old-password-1", "Grace Hopper"), null)
                .getBody().path("token").asText();
        String secondToken = post(LOGIN_URL, loginBody(email, "old-password-1"), null)
                .getBody().path("token").asText();

        assertThat(post(PASSWORD_URL, Map.of("currentPassword", "wrong-password",
                "newPassword", "new-password-1"), firstToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post(PASSWORD_URL, Map.of("currentPassword", "old-password-1",
                "newPassword", "new-password-1"), firstToken).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(post(LOGIN_URL, loginBody(email, "old-password-1"), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post(LOGIN_URL, loginBody(email, "new-password-1"), null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get(PROFILE_URL, firstToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(PROFILE_URL, secondToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validatesCredentialsAndKeepsPublicRoutesOpen() {
        assertThat(get(PROFILE_URL, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get(PROFILE_URL, "invalid-token").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post(LOGIN_URL, loginBody("missing@example.com", "password-1"), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<JsonNode> invalidEmail = post(REGISTER_URL,
                registerBody("not-an-email", "password-1", "Test User"), null);
        assertThat(invalidEmail.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidEmail.getBody().path("error").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(post(REGISTER_URL, registerBody("short@example.com", "short", "Test User"), null)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(get("/api/v1/jobs", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/admin/jobs", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Map<String, Object> registerBody(String email, String password, String fullName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("fullName", fullName);
        return body;
    }

    private Map<String, Object> loginBody(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.GET, entity(null, token), JsonNode.class);
    }

    private ResponseEntity<JsonNode> post(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.POST, entity(body, token), JsonNode.class);
    }

    private ResponseEntity<JsonNode> patch(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.PATCH, entity(body, token), JsonNode.class);
    }

    private ResponseEntity<JsonNode> delete(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.DELETE, entity(null, token), JsonNode.class);
    }

    private HttpEntity<Object> entity(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }
}
