package com.punestartup.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ScrapedRecord;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.entity.ScrapedRecordType;
import com.punestartup.ingestion.entity.ScrapingRun;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import com.punestartup.ingestion.entity.ScrapingRunTrigger;
import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.ingestion.entity.ScrapingSourceType;
import com.punestartup.ingestion.event.ScrapeRunCompletedEvent;
import com.punestartup.ingestion.messaging.IngestionCommandPublisher;
import com.punestartup.ingestion.repository.ScrapedRecordRepository;
import com.punestartup.ingestion.repository.ScrapingRunRepository;
import com.punestartup.ingestion.repository.ScrapingSourceRepository;
import com.punestartup.ingestion.service.ScrapingRunService;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.admin.api-key=test-admin-key", "app.ingestion.scheduler.initial-delay=PT24H"})
@Testcontainers
class ScrapingSourceApiIntegrationTest {

    private static final String ADMIN_KEY = "test-admin-key";
    private static final String SOURCES_URL = "/api/v1/admin/scraping-sources";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ScrapingSourceRepository sourceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ScrapingRunRepository runRepository;

    @Autowired
    private ScrapedRecordRepository recordRepository;

    @Autowired
    private ScrapingRunService runService;

    @MockitoBean
    private IngestionCommandPublisher commandPublisher;

    @BeforeEach
    void clean() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        recordRepository.deleteAll();
        runRepository.deleteAll();
        sourceRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void adminEndpointsRequireApiKeyButPublicEndpointsRemainOpen() {
        ResponseEntity<JsonNode> missing = restTemplate.getForEntity(SOURCES_URL, JsonNode.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missing.getBody().path("error").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(missing.getBody().path("path").asText()).isEqualTo(SOURCES_URL);

        ResponseEntity<JsonNode> wrong = exchange(HttpMethod.GET, SOURCES_URL, null, "wrong-key");
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<JsonNode> authorized = exchange(HttpMethod.GET, SOURCES_URL, null, ADMIN_KEY);
        assertThat(authorized.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> publicRequest = restTemplate.getForEntity("/api/v1/companies", JsonNode.class);
        assertThat(publicRequest.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createsSourceAsPendingAndDisabledByDefault() {
        ResponseEntity<JsonNode> response = create(createBody("Greenhouse Zeta", ScrapingSourceType.GREENHOUSE));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        JsonNode body = response.getBody();
        assertThat(body.path("sourceName").asText()).isEqualTo("Greenhouse Zeta");
        assertThat(body.path("sourceType").asText()).isEqualTo("GREENHOUSE");
        assertThat(body.path("permissionReviewStatus").asText()).isEqualTo("PENDING");
        assertThat(body.path("enabled").asBoolean()).isFalse();
        assertThat(body.path("companyId").isNull()).isTrue();
        assertThat(body.path("permissionNotes").asText()).isEqualTo("Public job board API");

        UUID id = UUID.fromString(body.path("id").asText());
        ResponseEntity<JsonNode> fetched = exchange(HttpMethod.GET, SOURCES_URL + "/" + id, null, ADMIN_KEY);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().path("id").asText()).isEqualTo(id.toString());
        assertThat(sourceRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsEnablingSourceThatIsNotApproved() {
        Map<String, Object> body = createBody("Pending Board", ScrapingSourceType.LEVER);
        body.put("enabled", true);

        ResponseEntity<JsonNode> response = create(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
        assertThat(sourceRepository.count()).isZero();
    }

    @Test
    void createsApprovedEnabledSourceLinkedToCompany() {
        Company company = company("ZetaSoft");
        Map<String, Object> body = createBody("Zeta Careers", ScrapingSourceType.COMPANY_CAREERS);
        body.put("companyId", company.getId().toString());
        body.put("permissionReviewStatus", "APPROVED");
        body.put("enabled", true);

        ResponseEntity<JsonNode> response = create(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().path("enabled").asBoolean()).isTrue();
        assertThat(response.getBody().path("companyId").asText()).isEqualTo(company.getId().toString());
        assertThat(response.getBody().path("companyName").asText()).isEqualTo("ZetaSoft");
    }

    @Test
    void changingStatusAwayFromApprovedDisablesSource() {
        Map<String, Object> body = createBody("Lever Zeta", ScrapingSourceType.LEVER);
        body.put("permissionReviewStatus", "APPROVED");
        body.put("enabled", true);
        ResponseEntity<JsonNode> created = create(body);
        String id = created.getBody().path("id").asText();
        assertThat(created.getBody().path("enabled").asBoolean()).isTrue();

        ResponseEntity<JsonNode> pending = update(id, Map.of("permissionReviewStatus", "PENDING"));
        assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pending.getBody().path("permissionReviewStatus").asText()).isEqualTo("PENDING");
        assertThat(pending.getBody().path("enabled").asBoolean()).isFalse();

        ResponseEntity<JsonNode> enablePending = update(id, Map.of("enabled", true));
        assertThat(enablePending.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(enablePending.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");

        Map<String, Object> approve = new LinkedHashMap<>();
        approve.put("permissionReviewStatus", "APPROVED");
        approve.put("enabled", true);
        approve.put("sourceName", "  Lever Zeta Pune  ");
        ResponseEntity<JsonNode> approved = update(id, approve);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody().path("enabled").asBoolean()).isTrue();
        assertThat(approved.getBody().path("sourceName").asText()).isEqualTo("Lever Zeta Pune");
    }

    @Test
    void listsAndFiltersSourcesSortedByName() {
        source("Zeta Feed", ScrapingSourceType.PARTNER_FEED, PermissionReviewStatus.APPROVED, true);
        source("alpha careers", ScrapingSourceType.COMPANY_CAREERS, PermissionReviewStatus.APPROVED, false);
        source("Beta Board", ScrapingSourceType.GREENHOUSE, PermissionReviewStatus.PENDING, false);

        JsonNode all = list("");
        assertThat(all.path("totalElements").asLong()).isEqualTo(3);
        assertThat(names(all.path("content"))).containsExactly("alpha careers", "Beta Board", "Zeta Feed");

        assertThat(list("?permissionStatus=APPROVED").path("totalElements").asLong()).isEqualTo(2);
        assertThat(list("?enabled=true").path("totalElements").asLong()).isEqualTo(1);
        assertThat(list("?sourceType=GREENHOUSE").path("totalElements").asLong()).isEqualTo(1);

        JsonNode keyword = list("?keyword=beta");
        assertThat(keyword.path("totalElements").asLong()).isEqualTo(1);
        assertThat(keyword.path("content").get(0).path("sourceName").asText()).isEqualTo("Beta Board");

        JsonNode paged = list("?page=1&size=2");
        assertThat(paged.path("content")).hasSize(1);
        assertThat(paged.path("page").asInt()).isEqualTo(1);
        assertThat(paged.path("totalPages").asInt()).isEqualTo(2);
        assertThat(paged.path("hasNext").asBoolean()).isFalse();
    }

    @Test
    void rejectsInvalidPayloadsAndUnknownReferences() {
        Map<String, Object> invalidUrl = createBody("Bad Url", ScrapingSourceType.OTHER);
        invalidUrl.put("baseUrl", "not-a-url");
        ResponseEntity<JsonNode> badUrl = create(invalidUrl);
        assertThat(badUrl.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badUrl.getBody().path("error").asText()).isEqualTo("VALIDATION_FAILED");

        Map<String, Object> badEnum = createBody("Bad Enum", ScrapingSourceType.OTHER);
        badEnum.put("sourceType", "NOT_A_TYPE");
        ResponseEntity<JsonNode> badEnumResponse = create(badEnum);
        assertThat(badEnumResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badEnumResponse.getBody().path("error").asText()).isEqualTo("INVALID_REQUEST");

        Map<String, Object> unknownCompany = createBody("Unknown Company", ScrapingSourceType.OTHER);
        unknownCompany.put("companyId", UUID.randomUUID().toString());
        ResponseEntity<JsonNode> companyResponse = create(unknownCompany);
        assertThat(companyResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(exchange(HttpMethod.GET, SOURCES_URL + "/" + UUID.randomUUID(), null, ADMIN_KEY)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(update(UUID.randomUUID().toString(), Map.of("enabled", false)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void retriesFailedRunAndTracksRetryLineage() {
        ScrapingSource source = source("Retry Board", ScrapingSourceType.GREENHOUSE,
                PermissionReviewStatus.APPROVED, true);
        Instant now = Instant.now();
        ScrapingRun original = run(source, ScrapingRunStatus.FAILED, now.minusSeconds(10), now.minusSeconds(9));

        ResponseEntity<JsonNode> response = exchange(HttpMethod.POST,
                "/api/v1/admin/scraping-runs/" + original.getId() + "/retry", null, ADMIN_KEY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        JsonNode body = response.getBody();
        assertThat(body.path("status").asText()).isEqualTo("RUNNING");
        assertThat(body.path("sourceId").asText()).isEqualTo(source.getId().toString());
        assertThat(body.path("retriedFromRunId").asText()).isEqualTo(original.getId().toString());
        assertThat(body.path("recordsAccepted").asInt()).isZero();

        ScrapingRun retried = runRepository.findById(UUID.fromString(body.path("id").asText())).orElseThrow();
        assertThat(retried.getRetriedFromRunId()).isEqualTo(original.getId());
        assertThat(runRepository.count()).isEqualTo(2);
    }

    @Test
    void rejectsRetryForRunningSuccessfulAndUnknownRuns() {
        Instant now = Instant.now();

        ScrapingSource runningSource = source("Running Board", ScrapingSourceType.GREENHOUSE,
                PermissionReviewStatus.APPROVED, true);
        ScrapingRun running = run(runningSource, ScrapingRunStatus.RUNNING, now.minusSeconds(5), null);
        ResponseEntity<JsonNode> runningRetry = exchange(HttpMethod.POST,
                "/api/v1/admin/scraping-runs/" + running.getId() + "/retry", null, ADMIN_KEY);
        assertThat(runningRetry.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(runningRetry.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");

        ScrapingSource successSource = source("Successful Board", ScrapingSourceType.LEVER,
                PermissionReviewStatus.APPROVED, true);
        ScrapingRun succeeded = run(successSource, ScrapingRunStatus.SUCCEEDED, now.minusSeconds(10), now.minusSeconds(9));
        ResponseEntity<JsonNode> successRetry = exchange(HttpMethod.POST,
                "/api/v1/admin/scraping-runs/" + succeeded.getId() + "/retry", null, ADMIN_KEY);
        assertThat(successRetry.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(successRetry.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");

        ResponseEntity<JsonNode> unknown = exchange(HttpMethod.POST,
                "/api/v1/admin/scraping-runs/" + UUID.randomUUID() + "/retry", null, ADMIN_KEY);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enforcesSourceRateLimitBetweenRunStarts() {
        ScrapingSource source = source("Rate Limited Board", ScrapingSourceType.GREENHOUSE,
                PermissionReviewStatus.APPROVED, true);
        source.setRateLimitPerMinute(1);
        sourceRepository.save(source);
        ScrapingRun recent = run(source, ScrapingRunStatus.FAILED, Instant.now(), Instant.now());

        ResponseEntity<JsonNode> response = exchange(HttpMethod.POST,
                SOURCES_URL + "/" + source.getId() + "/runs", null, ADMIN_KEY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().path("error").asText()).isEqualTo("RATE_LIMITED");
        assertThat(response.getHeaders().getFirst("Retry-After")).isNotNull();
        assertThat(runRepository.count()).isEqualTo(1);
        assertThat(recent.getStatus()).isEqualTo(ScrapingRunStatus.FAILED);
    }

    @Test
    void runDetailExposesAcceptedMetricAndFailedRecordErrors() {
        ScrapingSource source = source("Monitoring Board", ScrapingSourceType.GREENHOUSE,
                PermissionReviewStatus.APPROVED, true);
        Instant now = Instant.now();
        ScrapingRun run = run(source, ScrapingRunStatus.FAILED, now.minusSeconds(10), now.minusSeconds(9));
        run.setRecordsDiscovered(10);
        run.setRecordsEmitted(8);
        run.setRecordsProcessed(8);
        run.setRecordsValidated(5);
        run.setRecordsDuplicated(2);
        run.setRecordsRejected(1);
        run.setRecordsFailed(2);
        run.setErrorSummary("2 records could not be published");
        runRepository.save(run);
        record(run, "bad-1", ScrapedRecordStatus.FAILED, "job.title is required");
        record(run, "ok-1", ScrapedRecordStatus.PROCESSED, null);

        ResponseEntity<JsonNode> detail = exchange(HttpMethod.GET,
                "/api/v1/admin/scraping-runs/" + run.getId(), null, ADMIN_KEY);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = detail.getBody();
        assertThat(body.path("recordsAccepted").asInt()).isEqualTo(7);
        assertThat(body.path("recordsRejected").asInt()).isEqualTo(1);
        assertThat(body.path("recordsDuplicated").asInt()).isEqualTo(2);
        assertThat(body.path("recordsFailed").asInt()).isEqualTo(2);
        assertThat(body.path("retriedFromRunId").isNull()).isTrue();

        ResponseEntity<JsonNode> errors = exchange(HttpMethod.GET,
                "/api/v1/admin/scraping-runs/" + run.getId() + "/errors", null, ADMIN_KEY);
        assertThat(errors.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(errors.getBody().path("totalElements").asLong()).isEqualTo(1);
        assertThat(errors.getBody().path("content").get(0).path("externalId").asText()).isEqualTo("bad-1");
        assertThat(errors.getBody().path("content").get(0).path("errorMessage").asText())
                .isEqualTo("job.title is required");

        assertThat(exchange(HttpMethod.GET,
                "/api/v1/admin/scraping-runs/" + UUID.randomUUID() + "/errors", null, ADMIN_KEY)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void tracksConsecutiveFailuresAndAllowsDisablingFailingSource() {
        ScrapingSource source = source("Flaky Board", ScrapingSourceType.COMPANY_CAREERS,
                PermissionReviewStatus.APPROVED, true);

        ScrapingRun failed = run(source, ScrapingRunStatus.RUNNING, Instant.now().minusSeconds(5), null);
        complete(failed, source, "FAILED");
        ScrapingSource afterFailure = sourceRepository.findById(source.getId()).orElseThrow();
        assertThat(afterFailure.getConsecutiveFailedRuns()).isEqualTo(1);
        assertThat(afterFailure.getLastFailureAt()).isNotNull();

        ScrapingRun success = run(source, ScrapingRunStatus.RUNNING, Instant.now(), null);
        complete(success, source, "SUCCEEDED");
        ScrapingSource afterSuccess = sourceRepository.findById(source.getId()).orElseThrow();
        assertThat(afterSuccess.getConsecutiveFailedRuns()).isZero();
        assertThat(afterSuccess.getLastSuccessAt()).isNotNull();

        ScrapingRun failedAgain = run(source, ScrapingRunStatus.RUNNING, Instant.now(), null);
        complete(failedAgain, source, "FAILED");
        JsonNode health = exchange(HttpMethod.GET, SOURCES_URL + "/" + source.getId(), null, ADMIN_KEY).getBody();
        assertThat(health.path("consecutiveFailedRuns").asInt()).isEqualTo(1);
        assertThat(health.path("lastFailureAt").isNull()).isFalse();

        ResponseEntity<JsonNode> disabled = update(source.getId().toString(), Map.of("enabled", false));
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disabled.getBody().path("enabled").asBoolean()).isFalse();

        ResponseEntity<JsonNode> blocked = exchange(HttpMethod.POST,
                SOURCES_URL + "/" + source.getId() + "/runs", null, ADMIN_KEY);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(blocked.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void schedulesApprovedEnabledSourcesAndMarksScheduledRuns() {
        ScrapingSource source = source("Scheduled Board", ScrapingSourceType.GREENHOUSE,
                PermissionReviewStatus.APPROVED, true);

        runService.runDueSources(Instant.now());

        List<ScrapingRun> runs = runRepository.findAll();
        assertThat(runs).hasSize(1);
        assertThat(runs.getFirst().getStatus()).isEqualTo(ScrapingRunStatus.RUNNING);
        assertThat(runs.getFirst().getTriggerType()).isEqualTo(ScrapingRunTrigger.SCHEDULED);
    }

    @Test
    void recordsSkippedScheduledRunWhenSourceIsAlreadyRunning() {
        ScrapingSource source = source("Busy Board", ScrapingSourceType.LEVER,
                PermissionReviewStatus.APPROVED, true);
        source.setCrawlIntervalMinutes(1);
        sourceRepository.save(source);
        run(source, ScrapingRunStatus.RUNNING, Instant.now().minusSeconds(61), null);

        runService.runDueSources(Instant.now());

        assertThat(runRepository.findAll()).anySatisfy(scheduled -> {
            assertThat(scheduled.getStatus()).isEqualTo(ScrapingRunStatus.SKIPPED);
            assertThat(scheduled.getTriggerType()).isEqualTo(ScrapingRunTrigger.SCHEDULED);
            assertThat(scheduled.getFinishedAt()).isNotNull();
        });
    }

    @Test
    void recordsMissedRunBeforeStartingLateScheduledRun() {
        ScrapingSource source = source("Late Board", ScrapingSourceType.COMPANY_CAREERS,
                PermissionReviewStatus.APPROVED, true);
        source.setCrawlIntervalMinutes(1);
        sourceRepository.save(source);
        Instant startedAt = Instant.now().minusSeconds(16 * 60L);
        run(source, ScrapingRunStatus.SUCCEEDED, startedAt, startedAt.plusSeconds(10));

        runService.runDueSources(Instant.now());

        assertThat(runRepository.findAll()).anySatisfy(missed -> {
            assertThat(missed.getStatus()).isEqualTo(ScrapingRunStatus.MISSED);
            assertThat(missed.getTriggerType()).isEqualTo(ScrapingRunTrigger.SCHEDULED);
            assertThat(missed.getFinishedAt()).isNotNull();
        });
        assertThat(runRepository.findAll()).anySatisfy(scheduled -> {
            assertThat(scheduled.getStatus()).isEqualTo(ScrapingRunStatus.RUNNING);
            assertThat(scheduled.getTriggerType()).isEqualTo(ScrapingRunTrigger.SCHEDULED);
        });
    }

    private void complete(ScrapingRun run, ScrapingSource source, String status) {
        ScrapeRunCompletedEvent event = new ScrapeRunCompletedEvent(
                UUID.randomUUID(), "ScrapeRunCompleted", 1, Instant.now(), "test",
                run.getId(), source.getId(), status, 0, 0,
                "FAILED".equals(status) ? "source offline" : null);
        runService.completeRun(event);
    }

    private ScrapingRun run(ScrapingSource source, ScrapingRunStatus status, Instant startedAt, Instant finishedAt) {
        ScrapingRun run = new ScrapingRun();
        run.setId(UUID.randomUUID());
        run.setSource(source);
        run.setStatus(status);
        run.setStartedAt(startedAt);
        run.setFinishedAt(finishedAt);
        return runRepository.save(run);
    }

    private ScrapedRecord record(ScrapingRun run, String externalId, ScrapedRecordStatus status, String error) {
        ScrapedRecord record = new ScrapedRecord();
        record.setId(UUID.randomUUID());
        record.setRun(run);
        record.setRecordType(ScrapedRecordType.JOB);
        record.setExternalId(externalId);
        record.setSourceUrl("https://example.com/jobs/" + externalId);
        record.setPayload("{}");
        record.setNormalizedPayload("{}");
        record.setProcessingStatus(status);
        record.setErrorMessage(error);
        record.setProcessedAt(Instant.now());
        return recordRepository.save(record);
    }

    private Map<String, Object> createBody(String name, ScrapingSourceType type) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceName", name);
        body.put("sourceType", type.name());
        body.put("baseUrl", "https://boards.example.com/" + type.name().toLowerCase());
        body.put("crawlIntervalMinutes", 360);
        body.put("rateLimitPerMinute", 30);
        body.put("permissionNotes", "Public job board API");
        return body;
    }

    private ResponseEntity<JsonNode> create(Map<String, Object> body) {
        return exchange(HttpMethod.POST, SOURCES_URL, body, ADMIN_KEY);
    }

    private ResponseEntity<JsonNode> update(String id, Map<String, Object> body) {
        return exchange(HttpMethod.PATCH, SOURCES_URL + "/" + id, body, ADMIN_KEY);
    }

    private JsonNode list(String query) {
        ResponseEntity<JsonNode> response = exchange(HttpMethod.GET, SOURCES_URL + query, null, ADMIN_KEY);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String url, Object body, String adminKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (adminKey != null) {
            headers.set("X-Admin-Key", adminKey);
        }
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private List<String> names(JsonNode content) {
        return java.util.stream.StreamSupport.stream(content.spliterator(), false)
                .map(node -> node.path("sourceName").asText())
                .toList();
    }

    private ScrapingSource source(String name, ScrapingSourceType type, PermissionReviewStatus status,
                                  boolean enabled) {
        ScrapingSource source = new ScrapingSource();
        source.setId(UUID.randomUUID());
        source.setSourceName(name);
        source.setSourceType(type);
        source.setBaseUrl("https://example.com/" + UUID.randomUUID());
        source.setPermissionReviewStatus(status);
        source.setCrawlIntervalMinutes(60);
        source.setEnabled(enabled);
        return sourceRepository.save(source);
    }

    private Company company(String name) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setNormalizedName(name.toLowerCase());
        company.setSlug(name.toLowerCase().replace(' ', '-'));
        company.setCompanyType(CompanyType.STARTUP);
        company.setVerificationStatus(VerificationStatus.VERIFIED);
        company.setRecordStatus(RecordStatus.ACTIVE);
        return companyRepository.save(company);
    }
}
