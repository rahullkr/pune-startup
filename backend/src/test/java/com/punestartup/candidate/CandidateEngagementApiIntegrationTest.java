package com.punestartup.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.punestartup.auth.repository.AuthSessionRepository;
import com.punestartup.auth.repository.UserRepository;
import com.punestartup.auth.repository.UserRoleRepository;
import com.punestartup.candidate.repository.ApplicationRepository;
import com.punestartup.candidate.repository.ApplicationStatusHistoryRepository;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.CandidateSkillRepository;
import com.punestartup.candidate.repository.FollowedCompanyRepository;
import com.punestartup.candidate.repository.SavedJobRepository;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.GeocodeStatus;
import com.punestartup.company.entity.LocationType;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.company.repository.IndustryRepository;
import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSkillRepository;
import com.punestartup.job.repository.JobSourceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
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
class CandidateEngagementApiIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String SAVED_JOBS_URL = "/api/v1/candidates/me/saved-jobs";
    private static final String FOLLOWED_COMPANIES_URL = "/api/v1/candidates/me/followed-companies";
    private static final String APPLICATIONS_URL = "/api/v1/candidates/me/applications";
    private static final String DASHBOARD_URL = "/api/v1/candidates/me/dashboard";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

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
    private ApplicationStatusHistoryRepository historyRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private SavedJobRepository savedJobRepository;
    @Autowired
    private FollowedCompanyRepository followedCompanyRepository;
    @Autowired
    private JobSkillRepository jobSkillRepository;
    @Autowired
    private JobSourceRepository jobSourceRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private CompanyLocationRepository locationRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private IndustryRepository industryRepository;

    private String token;
    private Company company;
    private Company inactiveCompany;
    private Job activeJob;
    private Job expiredJob;

    @AfterEach
    void cleanEngagement() {
        historyRepository.deleteAll();
        applicationRepository.deleteAll();
        savedJobRepository.deleteAll();
        followedCompanyRepository.deleteAll();
    }

    @BeforeEach
    void cleanAndSeed() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        cleanEngagement();
        jobSkillRepository.deleteAll();
        jobSourceRepository.deleteAll();
        jobRepository.deleteAll();
        locationRepository.deleteAll();
        companyRepository.deleteAll();
        sessionRepository.deleteAll();
        candidateSkillRepository.deleteAll();
        profileRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        token = register("candidate@example.com", "Ada Lovelace");
        company = company("ZetaSoft", "zetasoft", RecordStatus.ACTIVE);
        inactiveCompany = company("Hidden Corp", "hidden-corp", RecordStatus.INACTIVE);
        CompanyLocation location = location(company, "Baner", "Pune");
        activeJob = job(company, location, "Backend Engineer", JobStatus.ACTIVE,
                Instant.now().minusSeconds(3600));
        expiredJob = job(company, location, "Legacy Role", JobStatus.EXPIRED,
                Instant.now().minusSeconds(7200));
    }

    @Test
    void savesAndUnsavesJobsIdempotently() {
        assertThat(get(SAVED_JOBS_URL, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<JsonNode> created = put(SAVED_JOBS_URL + "/" + activeJob.getId(), null, token);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().path("jobId").asText()).isEqualTo(activeJob.getId().toString());
        assertThat(created.getBody().path("available").asBoolean()).isTrue();
        assertThat(created.getBody().path("job").path("title").asText()).isEqualTo("Backend Engineer");

        ResponseEntity<JsonNode> repeated = put(SAVED_JOBS_URL + "/" + activeJob.getId(), null, token);
        assertThat(repeated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(SAVED_JOBS_URL + "/" + activeJob.getId(), token).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode saved = page(SAVED_JOBS_URL);
        assertThat(saved.path("totalElements").asLong()).isEqualTo(1);
        assertThat(saved.path("content").get(0).path("job").path("company").path("name").asText())
                .isEqualTo("ZetaSoft");

        JsonNode dashboard = get(DASHBOARD_URL, token).getBody();
        assertThat(dashboard.path("savedJobCount").asLong()).isEqualTo(1);
        assertThat(dashboard.path("recentSavedJobs")).hasSize(1);

        assertThat(put(SAVED_JOBS_URL + "/" + expiredJob.getId(), null, token).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(put(SAVED_JOBS_URL + "/" + UUID.randomUUID(), null, token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(delete(SAVED_JOBS_URL + "/" + activeJob.getId(), token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(delete(SAVED_JOBS_URL + "/" + activeJob.getId(), token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(SAVED_JOBS_URL + "/" + activeJob.getId(), token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(page(SAVED_JOBS_URL).path("totalElements").asLong()).isZero();
    }

    @Test
    void followsAndUnfollowsCompaniesWithActiveJobCounts() {
        ResponseEntity<JsonNode> created = put(FOLLOWED_COMPANIES_URL + "/" + company.getId(), null, token);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().path("companyId").asText()).isEqualTo(company.getId().toString());
        assertThat(created.getBody().path("activeJobCount").asLong()).isEqualTo(1);
        assertThat(created.getBody().path("available").asBoolean()).isTrue();

        assertThat(put(FOLLOWED_COMPANIES_URL + "/" + company.getId(), null, token).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get(FOLLOWED_COMPANIES_URL + "/" + company.getId(), token).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(put(FOLLOWED_COMPANIES_URL + "/" + inactiveCompany.getId(), null, token).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode followed = page(FOLLOWED_COMPANIES_URL);
        assertThat(followed.path("totalElements").asLong()).isEqualTo(1);
        assertThat(followed.path("content").get(0).path("name").asText()).isEqualTo("ZetaSoft");
        assertThat(followed.path("content").get(0).path("industryName").asText()).isEqualTo("SaaS");

        JsonNode dashboard = get(DASHBOARD_URL, token).getBody();
        assertThat(dashboard.path("followedCompanyCount").asLong()).isEqualTo(1);
        assertThat(dashboard.path("recentFollowedCompanies")).hasSize(1);

        assertThat(delete(FOLLOWED_COMPANIES_URL + "/" + company.getId(), token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(FOLLOWED_COMPANIES_URL + "/" + company.getId(), token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(page(FOLLOWED_COMPANIES_URL).path("totalElements").asLong()).isZero();
    }

    @Test
    void createsTracksAndDeletesApplications() {
        Map<String, Object> create = new LinkedHashMap<>();
        create.put("jobId", activeJob.getId());
        create.put("status", "APPLIED");
        create.put("notes", "Referred by a friend");
        create.put("externalApplicationUrl", "https://zetasoft.example/apply");
        create.put("statusNote", "Applied through the careers page");

        ResponseEntity<JsonNode> created = post(APPLICATIONS_URL, create, token);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode application = created.getBody();
        String applicationId = application.path("id").asText();
        assertThat(application.path("status").asText()).isEqualTo("APPLIED");
        assertThat(application.path("appliedAt").asText()).isNotBlank();
        assertThat(application.path("job").path("title").asText()).isEqualTo("Backend Engineer");
        assertThat(application.path("history")).hasSize(1);
        assertThat(application.path("history").get(0).path("oldStatus").isNull()).isTrue();
        assertThat(application.path("history").get(0).path("newStatus").asText()).isEqualTo("APPLIED");
        assertThat(application.path("history").get(0).path("note").asText())
                .isEqualTo("Applied through the careers page");

        assertThat(post(APPLICATIONS_URL, create, token).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        JsonNode listed = page(APPLICATIONS_URL);
        assertThat(listed.path("totalElements").asLong()).isEqualTo(1);
        assertThat(page(APPLICATIONS_URL + "?status=INTERVIEW").path("totalElements").asLong()).isZero();

        assertThat(get(APPLICATIONS_URL + "/" + applicationId, token).getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("status", "INTERVIEW");
        update.put("statusNote", "Call scheduled");
        ResponseEntity<JsonNode> updated = patch(APPLICATIONS_URL + "/" + applicationId, update, token);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("status").asText()).isEqualTo("INTERVIEW");
        assertThat(updated.getBody().path("history")).hasSize(2);
        assertThat(updated.getBody().path("history").get(1).path("oldStatus").asText()).isEqualTo("APPLIED");
        assertThat(updated.getBody().path("history").get(1).path("newStatus").asText()).isEqualTo("INTERVIEW");

        assertThat(page(APPLICATIONS_URL + "?status=INTERVIEW").path("totalElements").asLong()).isEqualTo(1);

        JsonNode dashboard = get(DASHBOARD_URL, token).getBody();
        assertThat(dashboard.path("applicationCount").asLong()).isEqualTo(1);
        assertThat(dashboard.path("recentApplications")).hasSize(1);
        assertThat(dashboard.path("recentApplications").get(0).path("status").asText())
                .isEqualTo("INTERVIEW");

        String otherToken = register("other@example.com", "Grace Hopper");
        assertThat(get(APPLICATIONS_URL + "/" + applicationId, otherToken).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(page(APPLICATIONS_URL, otherToken).path("totalElements").asLong()).isZero();

        assertThat(delete(APPLICATIONS_URL + "/" + applicationId, token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(APPLICATIONS_URL + "/" + applicationId, token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(historyRepository.count()).isZero();
    }

    @Test
    void validatesApplicationDatesUrlsAndOwnership() {
        Map<String, Object> saved = new LinkedHashMap<>();
        saved.put("jobId", activeJob.getId());
        saved.put("status", "SAVED");
        ResponseEntity<JsonNode> created = post(APPLICATIONS_URL, saved, token);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().path("appliedAt").isNull()).isTrue();

        Map<String, Object> invalidUrl = new LinkedHashMap<>();
        invalidUrl.put("jobId", expiredJob.getId());
        invalidUrl.put("externalApplicationUrl", "not-a-url");
        assertThat(post(APPLICATIONS_URL, invalidUrl, token).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String applicationId = created.getBody().path("id").asText();
        Map<String, Object> invalidUpdate = new LinkedHashMap<>();
        invalidUpdate.put("externalApplicationUrl", "ftp://example.com/apply");
        assertThat(patch(APPLICATIONS_URL + "/" + applicationId, invalidUpdate, token).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(delete(APPLICATIONS_URL + "/" + UUID.randomUUID(), token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
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

    private JsonNode page(String path) {
        return get(path, token).getBody();
    }

    private JsonNode page(String path, String authToken) {
        return get(path, authToken).getBody();
    }

    private Company company(String name, String slug, RecordStatus status) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setNormalizedName(name.toLowerCase());
        company.setSlug(slug);
        company.setDescription("Test company " + name);
        company.setCompanyType(CompanyType.STARTUP);
        company.setVerificationStatus(VerificationStatus.VERIFIED);
        company.setRecordStatus(status);
        company.setIndustry(industryRepository.findBySlug("saas").orElseThrow());
        return companyRepository.save(company);
    }

    private CompanyLocation location(Company company, String area, String city) {
        CompanyLocation location = new CompanyLocation();
        location.setId(UUID.randomUUID());
        location.setCompany(company);
        location.setArea(area);
        location.setCity(city);
        location.setState("Maharashtra");
        location.setCountryCode("IN");
        location.setLocationType(LocationType.OFFICE);
        location.setLocation(point(73.8567, 18.5204));
        location.setGeocodeStatus(GeocodeStatus.VERIFIED);
        location.setHeadquarters(true);
        return locationRepository.save(location);
    }

    private Job job(Company company, CompanyLocation location, String title, JobStatus status,
                    Instant firstPublishedAt) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setCompany(company);
        job.setCompanyLocation(location);
        job.setTitle(title);
        job.setNormalizedTitle(title.toLowerCase());
        job.setDescription("Build reliable software with Java and Spring Boot.");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setWorkMode(WorkMode.HYBRID);
        job.setExperienceMinYears(new BigDecimal("0"));
        job.setExperienceMaxYears(new BigDecimal("2"));
        job.setSalaryMin(new BigDecimal("800000"));
        job.setSalaryMax(new BigDecimal("1200000"));
        job.setSalaryCurrency("INR");
        job.setSalaryPeriod("YEAR");
        job.setStatus(status);
        job.setFirstPublishedAt(firstPublishedAt);
        job.setLastVerifiedAt(Instant.now());
        return jobRepository.save(job);
    }

    private Point point(double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.GET, entity(null, token), JsonNode.class);
    }

    private ResponseEntity<JsonNode> post(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.POST, entity(body, token), JsonNode.class);
    }

    private ResponseEntity<JsonNode> put(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.PUT, entity(body, token), JsonNode.class);
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
