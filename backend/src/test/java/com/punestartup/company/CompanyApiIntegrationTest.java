package com.punestartup.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.entity.CompanySource;
import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.GeocodeStatus;
import com.punestartup.company.entity.LocationType;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.company.repository.CompanySourceRepository;
import com.punestartup.company.repository.IndustryRepository;
import com.punestartup.common.web.RequestContext;
import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.job.repository.JobRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class CompanyApiIntegrationTest {

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
    private CompanySourceRepository sourceRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private IndustryRepository industryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private Company zetaSoft;
    private Company oldSchoolCorp;

    @BeforeEach
    void cleanAndSeed() {
        jobRepository.deleteAll();
        sourceRepository.deleteAll();
        locationRepository.deleteAll();
        companyRepository.deleteAll();

        zetaSoft = company("ZetaSoft", "zetasoft", VerificationStatus.VERIFIED);
        oldSchoolCorp = company("OldSchool Corp", "oldschool-corp", VerificationStatus.UNVERIFIED);
        Company approximate = company("Approx Co", "approx-co", VerificationStatus.VERIFIED);

        location(zetaSoft, "Hinjewadi Phase 2", "Pune", point(73.7275, 18.5913), true);
        location(oldSchoolCorp, "Baner", "Pune", point(73.7722, 18.5636), true);
        location(approximate, "Hinjewadi Phase 2", "Pune", point(73.7280, 18.5910), false,
                GeocodeStatus.APPROXIMATE);

        source(zetaSoft, "official_website", "https://zetasoft.example/about");
        source(zetaSoft, "linkedin", "https://linkedin.com/company/zetasoft");

        job(zetaSoft, "Backend Engineer", JobStatus.ACTIVE, "0", "2");
        job(zetaSoft, "Senior Data Engineer", JobStatus.ACTIVE, "4", "7");
        job(zetaSoft, "Legacy Role", JobStatus.EXPIRED, "0", "1");
        job(oldSchoolCorp, "Sales Executive", JobStatus.ACTIVE, "0", "1");
    }

    @Test
    void searchCompaniesReturnsSummariesWithJobCounts() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies?keyword=zetasoft", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = response.getBody().path("content");
        assertThat(content).hasSize(1);
        JsonNode summary = content.get(0);
        assertThat(summary.path("name").asText()).isEqualTo("ZetaSoft");
        assertThat(summary.path("slug").asText()).isEqualTo("zetasoft");
        assertThat(summary.path("verificationStatus").asText()).isEqualTo("VERIFIED");
        assertThat(summary.path("activeJobCount").asLong()).isEqualTo(2L);
        assertThat(summary.path("fresherJobCount").asLong()).isEqualTo(1L);
        assertThat(summary.path("areas").toString()).contains("Hinjewadi Phase 2");
        assertThat(response.getBody().path("totalElements").asLong()).isEqualTo(1L);
    }

    @Test
    void echoesRequestIdAndLogsRequestSummary(CapturedOutput output) {
        String successRequestId = "p6-success-" + UUID.randomUUID();
        HttpHeaders successHeaders = new HttpHeaders();
        successHeaders.set(RequestContext.REQUEST_ID_HEADER, successRequestId);

        ResponseEntity<JsonNode> success = restTemplate.exchange(
                "/api/v1/companies?keyword=zetasoft", HttpMethod.GET,
                new HttpEntity<>(successHeaders), JsonNode.class);

        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(success.getHeaders().getFirst(RequestContext.REQUEST_ID_HEADER))
                .isEqualTo(successRequestId);

        String errorRequestId = "p6-error-" + UUID.randomUUID();
        HttpHeaders errorHeaders = new HttpHeaders();
        errorHeaders.set(RequestContext.REQUEST_ID_HEADER, errorRequestId);

        ResponseEntity<JsonNode> error = restTemplate.exchange(
                "/api/v1/companies/not-a-uuid", HttpMethod.GET,
                new HttpEntity<>(errorHeaders), JsonNode.class);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.getBody().path("requestId").asText()).isEqualTo(errorRequestId);
        assertThat(error.getHeaders().getFirst(RequestContext.REQUEST_ID_HEADER)).isEqualTo(errorRequestId);
        assertThat(output.getAll())
                .contains("http_request method=GET path=/api/v1/companies status=200")
                .contains("requestId=" + successRequestId)
                .contains("http_request method=GET path=/api/v1/companies/not-a-uuid status=400")
                .contains("requestId=" + errorRequestId + " errorCode=INVALID_PARAMETER");
    }

    @Test
    void searchCompaniesFresherFilterReturnsOnlyCompaniesWithFresherJobs() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies?keyword=corp", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("content")).hasSize(1);

        response = restTemplate.getForEntity("/api/v1/companies?roleType=FRESHER", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = response.getBody().path("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).path("fresherJobCount").asLong()).isGreaterThan(0);
        assertThat(content.get(1).path("fresherJobCount").asLong()).isGreaterThan(0);
    }

    @Test
    void searchCompaniesByAreaFilter() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies?area=baner", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = response.getBody().path("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("name").asText()).isEqualTo("OldSchool Corp");
    }

    @Test
    void industriesEndpointReturnsIndustriesAlphabetically() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/industries", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.isArray()).isTrue();
        List<String> names = new ArrayList<>();
        body.forEach(node -> names.add(node.path("name").asText()));
        assertThat(names).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(names).contains("SaaS", "FinTech");
        assertThat(body.toString()).contains("\"slug\":\"saas\"");
    }

    @Test
    void companyAreasEndpointReturnsDistinctAreasOfActiveCompanies() {
        Company hidden = company("Hidden Co", "hidden-co", VerificationStatus.VERIFIED);
        hidden.setRecordStatus(RecordStatus.INACTIVE);
        companyRepository.save(hidden);
        location(hidden, "Kothrud", "Pune", point(73.8, 18.5), false);

        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies/areas", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> areas = new ArrayList<>();
        response.getBody().forEach(node -> areas.add(node.asText()));
        assertThat(areas).containsExactly("Baner", "Hinjewadi Phase 2");
        assertThat(areas).doesNotContain("Kothrud");
    }

    @Test
    void searchCompaniesSortUpdatedOrdersByMostRecentUpdate() {
        Company first = company("Sort Alpha", "sort-alpha", VerificationStatus.VERIFIED);
        Company second = company("Sort Beta", "sort-beta", VerificationStatus.VERIFIED);
        Instant now = Instant.now();

        jdbcTemplate.update("update companies set created_at = ?, updated_at = ? where id = ?",
                Timestamp.from(now.minusSeconds(7200)), Timestamp.from(now.minusSeconds(3600)), first.getId());
        jdbcTemplate.update("update companies set created_at = ?, updated_at = ? where id = ?",
                Timestamp.from(now.minusSeconds(3600)), Timestamp.from(now.minusSeconds(7200)), second.getId());

        ResponseEntity<JsonNode> updated = restTemplate.getForEntity(
                "/api/v1/companies?keyword=sort&sort=updated", JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("content").get(0).path("name").asText()).isEqualTo("Sort Alpha");

        ResponseEntity<JsonNode> newest = restTemplate.getForEntity(
                "/api/v1/companies?keyword=sort&sort=newest", JsonNode.class);
        assertThat(newest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newest.getBody().path("content").get(0).path("name").asText()).isEqualTo("Sort Beta");
    }

    @Test
    void searchCompaniesRejectsPageSizeAboveLimit() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies?size=100", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void searchCompaniesRejectsUnknownSort() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies?sort=bogus", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void companyDetailReturnsLocationsSourcesAndCounts() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies/{id}", JsonNode.class, zetaSoft.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.path("name").asText()).isEqualTo("ZetaSoft");
        assertThat(body.path("industryName").asText()).isEqualTo("SaaS");
        assertThat(body.path("locations")).hasSize(1);
        JsonNode location = body.path("locations").get(0);
        assertThat(location.path("area").asText()).isEqualTo("Hinjewadi Phase 2");
        assertThat(location.path("city").asText()).isEqualTo("Pune");
        assertThat(location.path("latitude").asDouble()).isEqualTo(18.5913);
        assertThat(location.path("longitude").asDouble()).isEqualTo(73.7275);
        assertThat(location.path("locationType").asText()).isEqualTo("OFFICE");
        assertThat(body.path("sources")).hasSize(2);
        assertThat(body.path("activeJobCount").asLong()).isEqualTo(2L);
        assertThat(body.path("fresherJobCount").asLong()).isEqualTo(1L);
    }

    @Test
    void companyDetailReturns404ForUnknownCompany() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies/{id}", JsonNode.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("error").asText()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void companyJobsReturnsOnlyActiveJobsWithFresherFlag() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/companies/{id}/jobs", JsonNode.class, zetaSoft.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = response.getBody().path("content");
        assertThat(content).hasSize(2);
        JsonNode backendRole = findJobByTitle(content, "Backend Engineer");
        assertThat(backendRole.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(backendRole.path("fresherEligible").asBoolean()).isTrue();
        JsonNode seniorRole = findJobByTitle(content, "Senior Data Engineer");
        assertThat(seniorRole.path("fresherEligible").asBoolean()).isFalse();
        assertThat(response.getBody().path("totalElements").asLong()).isEqualTo(2L);
    }

    @Test
    void mapMarkersReturnCompaniesWithinViewport() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/companies/map?minLat=18.55&maxLat=18.60&minLng=73.70&maxLng=73.75", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isArray()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        JsonNode marker = response.getBody().get(0);
        assertThat(marker.path("companyName").asText()).isEqualTo("ZetaSoft");
        assertThat(marker.path("activeJobCount").asLong()).isEqualTo(2L);
        assertThat(marker.path("fresherJobCount").asLong()).isEqualTo(1L);
        assertThat(marker.path("latitude").asDouble()).isEqualTo(18.5913);
        assertThat(response.getBody().toString()).doesNotContain("Approx Co");
    }

    @Test
    void mapMarkersRejectOversizedViewport() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/companies/map?minLat=10&maxLat=20&minLng=70&maxLng=80", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void nearbyCompaniesAreSortedByDistance() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/companies/nearby?lat=18.5636&lng=73.7722&radiusKm=50", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        JsonNode first = response.getBody().get(0);
        assertThat(first.path("companyName").asText()).isEqualTo("OldSchool Corp");
        assertThat(first.path("distanceMeters").asDouble()).isLessThan(500.0);
        JsonNode second = response.getBody().get(1);
        assertThat(second.path("companyName").asText()).isEqualTo("ZetaSoft");
        assertThat(second.path("distanceMeters").asDouble())
                .isGreaterThan(first.path("distanceMeters").asDouble());
    }

    @Test
    void healthEndpointIsAvailable() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
    }

    private Company company(String name, String slug, VerificationStatus verificationStatus) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setNormalizedName(name.toLowerCase());
        company.setSlug(slug);
        company.setDescription("Test company " + name);
        company.setCompanyType(CompanyType.STARTUP);
        company.setVerificationStatus(verificationStatus);
        company.setRecordStatus(RecordStatus.ACTIVE);
        company.setIndustry(industryRepository.findBySlug("saas").orElseThrow());
        company.setEmployeeCountMin(10);
        company.setEmployeeCountMax(50);
        company.setFundingStage("Series A");
        return companyRepository.save(company);
    }

    private void location(Company company, String area, String city, Point point, boolean headquarters) {
        location(company, area, city, point, headquarters, GeocodeStatus.VERIFIED);
    }

    private void location(Company company, String area, String city, Point point, boolean headquarters,
                          GeocodeStatus geocodeStatus) {
        CompanyLocation location = new CompanyLocation();
        location.setId(UUID.randomUUID());
        location.setCompany(company);
        location.setArea(area);
        location.setCity(city);
        location.setState("Maharashtra");
        location.setCountryCode("IN");
        location.setLocationType(LocationType.OFFICE);
        location.setLocation(point);
        location.setGeocodeStatus(geocodeStatus);
        location.setHeadquarters(headquarters);
        locationRepository.save(location);
    }

    private void source(Company company, String sourceName, String url) {
        CompanySource source = new CompanySource();
        source.setId(UUID.randomUUID());
        source.setCompany(company);
        source.setSourceName(sourceName);
        source.setSourceUrl(url);
        source.setLastVerifiedAt(Instant.now());
        sourceRepository.save(source);
    }

    private void job(Company company, String title, JobStatus status, String minYears, String maxYears) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setCompany(company);
        job.setTitle(title);
        job.setNormalizedTitle(title.toLowerCase());
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setWorkMode(WorkMode.HYBRID);
        job.setExperienceMinYears(new BigDecimal(minYears));
        job.setExperienceMaxYears(new BigDecimal(maxYears));
        job.setStatus(status);
        job.setFirstPublishedAt(Instant.now().minusSeconds(3600));
        jobRepository.save(job);
    }

    private Point point(double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private JsonNode findJobByTitle(JsonNode content, String title) {
        for (JsonNode node : content) {
            if (title.equals(node.path("title").asText())) {
                return node;
            }
        }
        throw new AssertionError("Job not found: " + title);
    }
}
