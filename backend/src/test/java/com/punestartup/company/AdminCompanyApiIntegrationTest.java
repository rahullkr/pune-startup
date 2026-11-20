package com.punestartup.company;

import com.fasterxml.jackson.databind.JsonNode;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanySource;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.company.repository.CompanySourceRepository;
import com.punestartup.job.repository.JobRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.admin.api-key=test-admin-key")
@Testcontainers
class AdminCompanyApiIntegrationTest {

    private static final String ADMIN_KEY = "test-admin-key";
    private static final String COMPANIES_URL = "/api/v1/admin/companies";

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

    @BeforeEach
    void clean() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        jobRepository.deleteAll();
        sourceRepository.deleteAll();
        locationRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void adminCompanyLifecycleCreatesDraftAndControlsPublicVisibility() {
        Map<String, Object> createBody = companyBody("Acme Rocket", "https://www.acmerocket.example/about", "saas");
        createBody.put("logoUrl", "https://cdn.example/acme.png");
        createBody.put("description", "Old description");
        createBody.put("foundedYear", 2020);
        createBody.put("employeeCountMin", 10);
        createBody.put("employeeCountMax", 50);
        createBody.put("fundingStage", "Seed");
        createBody.put("companyType", "STARTUP");
        createBody.put("verificationStatus", "UNVERIFIED");

        ResponseEntity<JsonNode> created = admin(HttpMethod.POST, COMPANIES_URL, createBody);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        JsonNode company = created.getBody();
        UUID id = UUID.fromString(company.path("id").asText());
        assertThat(company.path("recordStatus").asText()).isEqualTo("INACTIVE");
        assertThat(company.path("verificationStatus").asText()).isEqualTo("UNVERIFIED");
        assertThat(company.path("slug").asText()).isEqualTo("acme-rocket");
        assertThat(company.path("websiteDomain").asText()).isEqualTo("acmerocket.example");
        assertThat(company.path("locations")).isEmpty();
        assertThat(company.path("sources")).isEmpty();

        assertThat(restTemplate.getForEntity("/api/v1/companies/{id}", JsonNode.class, id).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/api/v1/companies?keyword=acme", JsonNode.class)
                .getBody().path("content")).isEmpty();

        Map<String, Object> location = locationBody("Baner", "Pune");
        location.put("locationName", "Acme HQ");
        location.put("addressLine", "Baner Road");
        location.put("locationType", "HEADQUARTERS");
        location.put("geocodeStatus", "VERIFIED");
        location.put("headquarters", true);
        location.put("latitude", 18.5636);
        location.put("longitude", 73.7722);

        ResponseEntity<JsonNode> locationCreated =
                admin(HttpMethod.POST, COMPANIES_URL + "/" + id + "/locations", location);
        assertThat(locationCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(locationCreated.getHeaders().getLocation()).isNotNull();
        assertThat(locationCreated.getBody().path("headquarters").asBoolean()).isTrue();
        assertThat(locationCreated.getBody().path("latitude").asDouble()).isEqualTo(18.5636);

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("name", "Acme Rocket Labs");
        update.put("websiteUrl", "https://acmerocketlabs.example");
        update.put("description", "Updated description");
        update.put("employeeCountMax", 75);
        update.put("verificationStatus", "VERIFIED");

        ResponseEntity<JsonNode> updated = admin(HttpMethod.PATCH, COMPANIES_URL + "/" + id, update);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("name").asText()).isEqualTo("Acme Rocket Labs");
        assertThat(updated.getBody().path("description").asText()).isEqualTo("Updated description");
        assertThat(updated.getBody().path("websiteDomain").asText()).isEqualTo("acmerocketlabs.example");
        assertThat(updated.getBody().path("verificationStatus").asText()).isEqualTo("VERIFIED");
        assertThat(updated.getBody().path("employeeCountMax").asInt()).isEqualTo(75);
        assertThat(updated.getBody().path("locations")).hasSize(1);

        ResponseEntity<JsonNode> published = admin(HttpMethod.POST, COMPANIES_URL + "/" + id + "/publish", null);
        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(published.getBody().path("recordStatus").asText()).isEqualTo("ACTIVE");

        ResponseEntity<JsonNode> publicCompany =
                restTemplate.getForEntity("/api/v1/companies/{id}", JsonNode.class, id);
        assertThat(publicCompany.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicCompany.getBody().path("name").asText()).isEqualTo("Acme Rocket Labs");
        assertThat(publicCompany.getBody().path("locations")).hasSize(1);
        assertThat(restTemplate.getForEntity("/api/v1/companies?keyword=acme", JsonNode.class)
                .getBody().path("content")).hasSize(1);

        ResponseEntity<JsonNode> unpublished = admin(HttpMethod.POST, COMPANIES_URL + "/" + id + "/unpublish", null);
        assertThat(unpublished.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unpublished.getBody().path("recordStatus").asText()).isEqualTo("INACTIVE");
        assertThat(restTemplate.getForEntity("/api/v1/companies/{id}", JsonNode.class, id).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<JsonNode> archived = admin(HttpMethod.POST, COMPANIES_URL + "/" + id + "/archive", null);
        assertThat(archived.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(archived.getBody().path("recordStatus").asText()).isEqualTo("ARCHIVED");
        assertThat(restTemplate.getForEntity("/api/v1/companies/{id}", JsonNode.class, id).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<JsonNode> adminDetail = admin(HttpMethod.GET, COMPANIES_URL + "/" + id, null);
        assertThat(adminDetail.getBody().path("recordStatus").asText()).isEqualTo("ARCHIVED");
        assertThat(adminDetail.getBody().path("locations")).hasSize(1);
    }

    @Test
    void rejectsDuplicateNamesAndDomainsAcrossCreateAndUpdate() {
        ResponseEntity<JsonNode> first = admin(HttpMethod.POST, COMPANIES_URL,
                companyBody("Duplicate Co", "https://www.duplicate.example/careers", "saas"));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID firstId = UUID.fromString(first.getBody().path("id").asText());

        ResponseEntity<JsonNode> duplicateName = admin(HttpMethod.POST, COMPANIES_URL,
                companyBody("duplicate co", "https://different.example", "saas"));
        assertThat(duplicateName.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateName.getBody().path("error").asText()).isEqualTo("CONSTRAINT_VIOLATION");
        assertThat(duplicateName.getBody().path("message").asText()).contains(firstId.toString());

        ResponseEntity<JsonNode> duplicateDomain = admin(HttpMethod.POST, COMPANIES_URL,
                companyBody("Other Co", "http://duplicate.example/jobs", "saas"));
        assertThat(duplicateDomain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<JsonNode> second = admin(HttpMethod.POST, COMPANIES_URL,
                companyBody("Other Co", "https://other.example", "saas"));
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID secondId = UUID.fromString(second.getBody().path("id").asText());

        ResponseEntity<JsonNode> byName = admin(HttpMethod.GET,
                COMPANIES_URL + "/duplicates?name=duplicate co", null);
        assertThat(byName.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byName.getBody()).hasSize(1);
        assertThat(byName.getBody().get(0).path("id").asText()).isEqualTo(firstId.toString());

        ResponseEntity<JsonNode> byDomain = admin(HttpMethod.GET,
                COMPANIES_URL + "/duplicates?websiteUrl=https://duplicate.example/about", null);
        assertThat(byDomain.getBody()).hasSize(1);
        assertThat(byDomain.getBody().get(0).path("id").asText()).isEqualTo(firstId.toString());

        ResponseEntity<JsonNode> excluded = admin(HttpMethod.GET,
                COMPANIES_URL + "/duplicates?name=duplicate co&excludeId=" + firstId, null);
        assertThat(excluded.getBody()).isEmpty();

        ResponseEntity<JsonNode> updateConflict = admin(HttpMethod.PATCH, COMPANIES_URL + "/" + secondId,
                Map.of("websiteUrl", "https://duplicate.example"));
        assertThat(updateConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<JsonNode> updateSelf = admin(HttpMethod.PATCH, COMPANIES_URL + "/" + secondId,
                Map.of("name", "Other Co", "websiteUrl", "https://other.example"));
        assertThat(updateSelf.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> missingCriteria = admin(HttpMethod.GET, COMPANIES_URL + "/duplicates", null);
        assertThat(missingCriteria.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingCriteria.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void updatesLocationAndEnforcesAtMostOneHeadquarters() {
        UUID companyId = createDraftCompany("Location Labs");
        UUID firstId = addLocation(companyId, "Baner", "Pune", true, 18.5636, 73.7722);

        ResponseEntity<JsonNode> second = admin(HttpMethod.POST, COMPANIES_URL + "/" + companyId + "/locations",
                locationBody("Hinjewadi Phase 2", "Pune"));
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID secondId = UUID.fromString(second.getBody().path("id").asText());
        assertThat(second.getBody().path("headquarters").asBoolean()).isFalse();

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("area", "Hinjewadi Phase 2");
        update.put("headquarters", true);
        update.put("geocodeStatus", "APPROXIMATE");
        update.put("latitude", 18.591);
        update.put("longitude", 73.728);
        ResponseEntity<JsonNode> updated =
                admin(HttpMethod.PATCH, COMPANIES_URL + "/" + companyId + "/locations/" + secondId, update);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("area").asText()).isEqualTo("Hinjewadi Phase 2");
        assertThat(updated.getBody().path("geocodeStatus").asText()).isEqualTo("APPROXIMATE");
        assertThat(updated.getBody().path("headquarters").asBoolean()).isTrue();
        assertThat(updated.getBody().path("latitude").asDouble()).isEqualTo(18.591);

        ResponseEntity<JsonNode> detail = admin(HttpMethod.GET, COMPANIES_URL + "/" + companyId, null);
        JsonNode locations = detail.getBody().path("locations");
        assertThat(locations).hasSize(2);
        assertThat(locations.get(0).path("id").asText()).isEqualTo(secondId.toString());
        assertThat(locations.get(0).path("headquarters").asBoolean()).isTrue();
        assertThat(locations.get(1).path("id").asText()).isEqualTo(firstId.toString());
        assertThat(locations.get(1).path("headquarters").asBoolean()).isFalse();

        ResponseEntity<JsonNode> latitudeOnly = admin(HttpMethod.PATCH,
                COMPANIES_URL + "/" + companyId + "/locations/" + firstId, Map.of("latitude", 18.5));
        assertThat(latitudeOnly.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(latitudeOnly.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void adminCompanyEndpointsRequireApiKey() {
        ResponseEntity<JsonNode> missing = restTemplate.getForEntity(COMPANIES_URL, JsonNode.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missing.getBody().path("error").asText()).isEqualTo("UNAUTHORIZED");

        ResponseEntity<JsonNode> wrong = exchange(HttpMethod.GET, COMPANIES_URL, null, "wrong-key");
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<JsonNode> authorized = admin(HttpMethod.GET, COMPANIES_URL, null);
        assertThat(authorized.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> publicRequest = restTemplate.getForEntity("/api/v1/companies", JsonNode.class);
        assertThat(publicRequest.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminListsFilterAndExposeSourceMetadata() {
        Map<String, Object> alphaBody = companyBody("Alpha SaaS", "https://alpha.example", "saas");
        alphaBody.put("verificationStatus", "PENDING_REVIEW");
        ResponseEntity<JsonNode> alpha = admin(HttpMethod.POST, COMPANIES_URL, alphaBody);
        UUID alphaId = UUID.fromString(alpha.getBody().path("id").asText());
        admin(HttpMethod.POST, COMPANIES_URL + "/" + alphaId + "/publish", null);

        Map<String, Object> betaBody = companyBody("Beta Fin", "https://beta.example", "fintech");
        betaBody.put("verificationStatus", "VERIFIED");
        ResponseEntity<JsonNode> beta = admin(HttpMethod.POST, COMPANIES_URL, betaBody);
        UUID betaId = UUID.fromString(beta.getBody().path("id").asText());

        ResponseEntity<JsonNode> all = admin(HttpMethod.GET, COMPANIES_URL, null);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(all.getBody().path("totalElements").asLong()).isEqualTo(2);
        assertThat(all.getBody().path("content").get(0).path("name").asText()).isEqualTo("Alpha SaaS");

        assertThat(admin(HttpMethod.GET, COMPANIES_URL + "?recordStatus=ACTIVE", null)
                .getBody().path("content")).hasSize(1);
        assertThat(admin(HttpMethod.GET, COMPANIES_URL + "?verificationStatus=VERIFIED", null)
                .getBody().path("content").get(0).path("id").asText()).isEqualTo(betaId.toString());
        assertThat(admin(HttpMethod.GET, COMPANIES_URL + "?industry=fintech", null)
                .getBody().path("content").get(0).path("id").asText()).isEqualTo(betaId.toString());
        assertThat(admin(HttpMethod.GET, COMPANIES_URL + "?keyword=alpha", null)
                .getBody().path("content")).hasSize(1);

        ResponseEntity<JsonNode> badSort = admin(HttpMethod.GET, COMPANIES_URL + "?sort=bogus", null);
        assertThat(badSort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<JsonNode> tooLarge = admin(HttpMethod.GET, COMPANIES_URL + "?size=100", null);
        assertThat(tooLarge.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Company alphaCompany = companyRepository.findById(alphaId).orElseThrow();
        CompanySource source = new CompanySource();
        source.setId(UUID.randomUUID());
        source.setCompany(alphaCompany);
        source.setSourceName("manual_review");
        source.setSourceUrl("https://source.example/alpha");
        source.setExternalId("alpha-1");
        source.setLastVerifiedAt(Instant.now());
        source.setSourceMetadata("{\"reviewer\":\"admin\"}");
        sourceRepository.save(source);

        ResponseEntity<JsonNode> detail = admin(HttpMethod.GET, COMPANIES_URL + "/" + alphaId, null);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().path("verificationStatus").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(detail.getBody().path("sources")).hasSize(1);
        assertThat(detail.getBody().path("sources").get(0).path("sourceMetadata").asText())
                .contains("\"reviewer\"")
                .contains("\"admin\"");

        ResponseEntity<JsonNode> publicDetail =
                restTemplate.getForEntity("/api/v1/companies/{id}", JsonNode.class, alphaId);
        assertThat(publicDetail.getBody().path("sources").get(0).path("sourceMetadata").isMissingNode()).isTrue();
    }

    @Test
    void validatesCompanyAndLocationFields() {
        Map<String, Object> future = companyBody("Future Co", "https://future.example", "saas");
        future.put("foundedYear", 2100);
        assertThat(admin(HttpMethod.POST, COMPANIES_URL, future).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> badRange = companyBody("Range Co", "https://range.example", "saas");
        badRange.put("employeeCountMin", 100);
        badRange.put("employeeCountMax", 10);
        assertThat(admin(HttpMethod.POST, COMPANIES_URL, badRange).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> unknownIndustry = companyBody("Industry Co", "https://industry.example", "unknown");
        ResponseEntity<JsonNode> industryResponse = admin(HttpMethod.POST, COMPANIES_URL, unknownIndustry);
        assertThat(industryResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(industryResponse.getBody().path("error").asText()).isEqualTo("RESOURCE_NOT_FOUND");

        Map<String, Object> noSlug = companyBody("!!!", "https://symbols.example", "saas");
        noSlug.remove("slug");
        ResponseEntity<JsonNode> slugResponse = admin(HttpMethod.POST, COMPANIES_URL, noSlug);
        assertThat(slugResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(slugResponse.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");

        UUID companyId = createDraftCompany("Validated Labs");
        Map<String, Object> verifiedWithoutCoordinates = locationBody("Baner", "Pune");
        verifiedWithoutCoordinates.remove("latitude");
        verifiedWithoutCoordinates.remove("longitude");
        verifiedWithoutCoordinates.put("geocodeStatus", "VERIFIED");
        ResponseEntity<JsonNode> verifiedResponse = admin(HttpMethod.POST,
                COMPANIES_URL + "/" + companyId + "/locations", verifiedWithoutCoordinates);
        assertThat(verifiedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(verifiedResponse.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");

        ResponseEntity<JsonNode> badEmployeeRange = admin(HttpMethod.PATCH, COMPANIES_URL + "/" + companyId,
                Map.of("employeeCountMin", 100));
        assertThat(badEmployeeRange.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private UUID createDraftCompany(String name) {
        Map<String, Object> body = companyBody(name, "https://" + name.toLowerCase().replace(' ', '-') + ".example",
                "saas");
        ResponseEntity<JsonNode> created = admin(HttpMethod.POST, COMPANIES_URL, body);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(created.getBody().path("id").asText());
    }

    private UUID addLocation(UUID companyId, String area, String city, boolean headquarters,
                             double latitude, double longitude) {
        Map<String, Object> body = locationBody(area, city);
        body.put("headquarters", headquarters);
        body.put("latitude", latitude);
        body.put("longitude", longitude);
        ResponseEntity<JsonNode> created = admin(HttpMethod.POST, COMPANIES_URL + "/" + companyId + "/locations", body);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(created.getBody().path("id").asText());
    }

    private Map<String, Object> companyBody(String name, String websiteUrl, String industrySlug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("websiteUrl", websiteUrl);
        body.put("industrySlug", industrySlug);
        body.put("description", "Test company " + name);
        body.put("employeeCountMin", 10);
        body.put("employeeCountMax", 50);
        return body;
    }

    private Map<String, Object> locationBody(String area, String city) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("area", area);
        body.put("city", city);
        body.put("state", "Maharashtra");
        body.put("countryCode", "IN");
        body.put("postalCode", "411045");
        body.put("locationType", "OFFICE");
        body.put("geocodeStatus", "VERIFIED");
        body.put("headquarters", false);
        body.put("latitude", 18.5636);
        body.put("longitude", 73.7722);
        return body;
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
