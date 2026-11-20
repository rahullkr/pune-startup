package com.punestartup.job;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSkillRepository;
import com.punestartup.job.repository.JobSourceRepository;
import com.punestartup.job.repository.JobStatusHistoryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class AdminJobApiIntegrationTest {

    private static final String ADMIN_KEY = "test-admin-key";
    private static final String JOBS_URL = "/api/v1/admin/jobs";
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private IndustryRepository industryRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSourceRepository jobSourceRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private JobStatusHistoryRepository statusHistoryRepository;

    private Company company;
    private CompanyLocation location;

    @BeforeEach
    void cleanAndSeed() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        statusHistoryRepository.deleteAll();
        jobSkillRepository.deleteAll();
        jobSourceRepository.deleteAll();
        jobRepository.deleteAll();
        locationRepository.deleteAll();
        companyRepository.deleteAll();

        company = company("Acme Pune", "acme-pune");
        location = location(company, "Baner");
    }

    @Test
    void createsReviewJobAndControlsLifecycle() {
        ResponseEntity<JsonNode> created = admin(HttpMethod.POST, JOBS_URL, jobBody(company.getId(), location.getId(),
                "Backend Engineer"));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        UUID jobId = UUID.fromString(created.getBody().path("id").asText());
        JsonNode job = created.getBody();
        assertThat(job.path("status").asText()).isEqualTo("NEEDS_REVIEW");
        assertThat(job.path("employmentType").asText()).isEqualTo("FULL_TIME");
        assertThat(job.path("workMode").asText()).isEqualTo("HYBRID");
        assertThat(job.path("salaryCurrency").asText()).isEqualTo("INR");
        assertThat(job.path("postedAt").isNull()).isFalse();
        assertThat(job.path("skills")).hasSize(2);
        assertThat(job.path("sources")).hasSize(1);
        assertThat(job.path("history")).isEmpty();
        assertThat(job.path("company").path("id").asText()).isEqualTo(company.getId().toString());
        assertThat(job.path("location").path("id").asText()).isEqualTo(location.getId().toString());

        assertThat(restTemplate.getForEntity("/api/v1/jobs/{id}", JsonNode.class, jobId).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<JsonNode> published = admin(HttpMethod.POST, JOBS_URL + "/" + jobId + "/publish", null);
        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(published.getBody().path("status").asText()).isEqualTo("ACTIVE");
        assertThat(published.getBody().path("firstPublishedAt").isNull()).isFalse();
        assertThat(published.getBody().path("lastVerifiedAt").isNull()).isFalse();
        assertThat(published.getBody().path("history")).hasSize(1);
        assertThat(published.getBody().path("history").get(0).path("newStatus").asText()).isEqualTo("ACTIVE");

        ResponseEntity<JsonNode> publicJob =
                restTemplate.getForEntity("/api/v1/jobs/{id}", JsonNode.class, jobId);
        assertThat(publicJob.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicJob.getBody().path("title").asText()).isEqualTo("Backend Engineer");

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("title", "Senior Backend Engineer");
        update.put("workMode", "REMOTE");
        update.put("clearSalary", true);
        update.put("skills", List.of(Map.of("name", "Java", "requirementType", "REQUIRED")));
        ResponseEntity<JsonNode> updated = admin(HttpMethod.PATCH, JOBS_URL + "/" + jobId, update);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("title").asText()).isEqualTo("Senior Backend Engineer");
        assertThat(updated.getBody().path("workMode").asText()).isEqualTo("REMOTE");
        assertThat(updated.getBody().path("salaryMin").isNull()).isTrue();
        assertThat(updated.getBody().path("salaryCurrency").isNull()).isTrue();
        assertThat(updated.getBody().path("skills")).hasSize(1);
        assertThat(updated.getBody().path("skills").get(0).path("skillName").asText()).isEqualTo("Java");

        ResponseEntity<JsonNode> stale = admin(HttpMethod.POST, JOBS_URL + "/" + jobId + "/mark-stale", null);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stale.getBody().path("status").asText()).isEqualTo("STALE");
        assertThat(stale.getBody().path("staleSince").isNull()).isFalse();

        ResponseEntity<JsonNode> expired = admin(HttpMethod.POST, JOBS_URL + "/" + jobId + "/expire", null);
        assertThat(expired.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(expired.getBody().path("status").asText()).isEqualTo("EXPIRED");
        assertThat(expired.getBody().path("expiredAt").isNull()).isFalse();

        ResponseEntity<JsonNode> removed = admin(HttpMethod.POST, JOBS_URL + "/" + jobId + "/remove", null);
        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removed.getBody().path("status").asText()).isEqualTo("REJECTED");
        assertThat(removed.getBody().path("sources").get(0).path("sourceStatus").asText()).isEqualTo("REMOVED");
        assertThat(restTemplate.getForEntity("/api/v1/jobs/{id}", JsonNode.class, jobId).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void detectsDuplicatesAndMergesSourcesAndSkills() {
        UUID targetId = createJob("Backend Engineer", "ext-a", "https://acme.example/jobs/a",
                List.of(Map.of("name", "Java", "requirementType", "REQUIRED")));
        UUID duplicateId = createJob("backend engineer", "ext-b", "https://acme.example/jobs/b",
                List.of(Map.of("name", "Kotlin", "requirementType", "REQUIRED")));

        ResponseEntity<JsonNode> byTitle = admin(HttpMethod.GET,
                JOBS_URL + "/duplicates?companyId=" + company.getId() + "&title=backend engineer", null);
        assertThat(byTitle.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(byTitle.getBody())).contains(targetId.toString(), duplicateId.toString());

        ResponseEntity<JsonNode> byUrl = admin(HttpMethod.GET,
                JOBS_URL + "/duplicates?applicationUrl=https://acme.example/jobs/b", null);
        assertThat(ids(byUrl.getBody())).containsExactly(duplicateId.toString());

        ResponseEntity<JsonNode> excluded = admin(HttpMethod.GET,
                JOBS_URL + "/duplicates?applicationUrl=https://acme.example/jobs/b&excludeId=" + duplicateId, null);
        assertThat(excluded.getBody()).isEmpty();

        ResponseEntity<JsonNode> missingCriteria = admin(HttpMethod.GET, JOBS_URL + "/duplicates", null);
        assertThat(missingCriteria.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<JsonNode> merged = admin(HttpMethod.POST, JOBS_URL + "/" + targetId + "/merge",
                Map.of("duplicateJobId", duplicateId.toString(), "note", "same role"));
        assertThat(merged.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(merged.getBody().path("id").asText()).isEqualTo(targetId.toString());
        assertThat(merged.getBody().path("sources")).hasSize(2);
        assertThat(merged.getBody().path("skills")).hasSize(2);

        ResponseEntity<JsonNode> duplicate = admin(HttpMethod.GET, JOBS_URL + "/" + duplicateId, null);
        assertThat(duplicate.getBody().path("status").asText()).isEqualTo("REJECTED");
        assertThat(duplicate.getBody().path("history")).hasSize(1);
        assertThat(duplicate.getBody().path("history").get(0).path("reason").asText())
                .contains("Merged into job " + targetId)
                .contains("same role");
        assertThat(jobSourceRepository.findByJob_IdOrderByFirstSeenAtAsc(duplicateId)).isEmpty();
        assertThat(jobSkillRepository.findByJob_Id(duplicateId)).isEmpty();
    }

    @Test
    void flagsDuplicatesAndValidatesJobFields() {
        UUID firstId = createJob("Backend Engineer", "ext-1", "https://acme.example/jobs/1",
                List.of(Map.of("name", "Java", "requirementType", "REQUIRED")));
        UUID secondId = createJob("Data Engineer", "ext-2", "https://acme.example/jobs/2",
                List.of(Map.of("name", "Python", "requirementType", "REQUIRED")));

        ResponseEntity<JsonNode> flagged = admin(HttpMethod.POST, JOBS_URL + "/" + firstId + "/flag-duplicate",
                Map.of("duplicateOfJobId", secondId.toString(), "note", "looks similar"));
        assertThat(flagged.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(flagged.getBody().path("status").asText()).isEqualTo("NEEDS_REVIEW");
        assertThat(flagged.getBody().path("history")).hasSize(1);
        assertThat(flagged.getBody().path("history").get(0).path("reason").asText())
                .contains(secondId.toString())
                .contains("looks similar");

        Map<String, Object> badSalary = jobBody(company.getId(), location.getId(), "Bad Salary");
        badSalary.put("salaryMin", 2000000);
        badSalary.put("salaryMax", 1000000);
        ResponseEntity<JsonNode> badSalaryResponse = admin(HttpMethod.POST, JOBS_URL, badSalary);
        assertThat(badSalaryResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badSalaryResponse.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");

        Map<String, Object> unknownSkill = jobBody(company.getId(), location.getId(), "Unknown Skill");
        unknownSkill.put("skills", List.of(Map.of("name", "COBOL 9000")));
        ResponseEntity<JsonNode> unknownSkillResponse = admin(HttpMethod.POST, JOBS_URL, unknownSkill);
        assertThat(unknownSkillResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownSkillResponse.getBody().path("error").asText()).isEqualTo("RESOURCE_NOT_FOUND");

        Company other = company("Other Pune", "other-pune");
        CompanyLocation otherLocation = location(other, "Kharadi");
        Map<String, Object> foreignLocation = jobBody(company.getId(), otherLocation.getId(), "Wrong Location");
        ResponseEntity<JsonNode> foreignLocationResponse = admin(HttpMethod.POST, JOBS_URL, foreignLocation);
        assertThat(foreignLocationResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<JsonNode> staleFromReview =
                admin(HttpMethod.POST, JOBS_URL + "/" + firstId + "/mark-stale", null);
        assertThat(staleFromReview.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(admin(HttpMethod.GET, JOBS_URL + "?sort=bogus", null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(admin(HttpMethod.GET, JOBS_URL + "?size=100", null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void adminJobEndpointsRequireApiKey() {
        ResponseEntity<JsonNode> missing = restTemplate.getForEntity(JOBS_URL, JsonNode.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missing.getBody().path("error").asText()).isEqualTo("UNAUTHORIZED");

        ResponseEntity<JsonNode> wrong = exchange(HttpMethod.GET, JOBS_URL, null, "wrong-key");
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(admin(HttpMethod.GET, JOBS_URL, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private UUID createJob(String title, String sourceJobId, String applicationUrl, List<Map<String, Object>> skills) {
        Map<String, Object> body = jobBody(company.getId(), location.getId(), title);
        body.put("skills", skills);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("sourceName", "official_website");
        source.put("sourceJobId", sourceJobId);
        source.put("sourceUrl", "https://acme.example/jobs/" + sourceJobId);
        source.put("applicationUrl", applicationUrl);
        body.put("source", source);
        ResponseEntity<JsonNode> created = admin(HttpMethod.POST, JOBS_URL, body);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(created.getBody().path("id").asText());
    }

    private Map<String, Object> jobBody(UUID companyId, UUID locationId, String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("companyId", companyId.toString());
        body.put("companyLocationId", locationId.toString());
        body.put("title", title);
        body.put("description", "Build reliable services for " + title);
        body.put("employmentType", "FULL_TIME");
        body.put("workMode", "HYBRID");
        body.put("experienceMinYears", 1.0);
        body.put("experienceMaxYears", 3.0);
        body.put("salaryMin", 800000);
        body.put("salaryMax", 1200000);
        body.put("salaryCurrency", "inr");
        body.put("salaryPeriod", "year");
        body.put("skills", List.of(
                Map.of("name", "Java", "requirementType", "REQUIRED"),
                Map.of("name", "Spring Boot", "requirementType", "PREFERRED")));
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("sourceName", "official_website");
        source.put("sourceJobId", "default-" + UUID.randomUUID());
        source.put("sourceUrl", "https://acme.example/jobs/default");
        body.put("source", source);
        return body;
    }

    private List<String> ids(JsonNode array) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        array.forEach(node -> ids.add(node.path("id").asText()));
        return ids;
    }

    private Company company(String name, String slug) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setNormalizedName(name.toLowerCase());
        company.setSlug(slug);
        company.setDescription("Test company " + name);
        company.setCompanyType(CompanyType.STARTUP);
        company.setVerificationStatus(VerificationStatus.VERIFIED);
        company.setRecordStatus(RecordStatus.ACTIVE);
        company.setIndustry(industryRepository.findBySlug("saas").orElseThrow());
        return companyRepository.save(company);
    }

    private CompanyLocation location(Company company, String area) {
        CompanyLocation location = new CompanyLocation();
        location.setId(UUID.randomUUID());
        location.setCompany(company);
        location.setLocationName(company.getName() + " " + area + " Office");
        location.setArea(area);
        location.setCity("Pune");
        location.setState("Maharashtra");
        location.setCountryCode("IN");
        location.setLocationType(LocationType.OFFICE);
        location.setLocation(newPoint(73.7722, 18.5636));
        location.setGeocodeStatus(GeocodeStatus.VERIFIED);
        location.setHeadquarters(true);
        return locationRepository.save(location);
    }

    private Point newPoint(double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private ResponseEntity<JsonNode> admin(HttpMethod method, String url, Object body) {
        return exchange(method, url, body, ADMIN_KEY);
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String url, Object body, String adminKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (adminKey != null) {
            headers.set("X-Admin-Key", adminKey);
        }
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
