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
import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobSkill;
import com.punestartup.job.entity.JobSkillId;
import com.punestartup.job.entity.JobSource;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.SkillRequirementType;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSkillRepository;
import com.punestartup.job.repository.JobSourceRepository;
import com.punestartup.skill.repository.SkillRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class JobApiIntegrationTest {

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
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private JobSourceRepository jobSourceRepository;

    @Autowired
    private SkillRepository skillRepository;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private Company zetaSoft;
    private Company banerFintech;
    private Job backendJob;
    private Job seniorJob;
    private Job frontendJob;
    private Job expiredJob;
    private Job hiddenJob;
    private Job oldJob;

    @BeforeEach
    void cleanAndSeed() {
        jobSkillRepository.deleteAll();
        jobSourceRepository.deleteAll();
        jobRepository.deleteAll();
        locationRepository.deleteAll();
        companyRepository.deleteAll();

        zetaSoft = company("ZetaSoft", "zetasoft", "saas", RecordStatus.ACTIVE);
        banerFintech = company("Baner Fintech Labs", "baner-fintech-labs", "fintech", RecordStatus.ACTIVE);
        Company hiddenCorp = company("Hidden Corp", "hidden-corp", "saas", RecordStatus.INACTIVE);

        CompanyLocation hinjewadi = location(zetaSoft, "Hinjewadi Phase 2", "Pune");
        CompanyLocation baner = location(banerFintech, "Baner", "Pune");
        CompanyLocation hiddenLocation = location(hiddenCorp, "Hinjewadi", "Pune");

        backendJob = job(zetaSoft, hinjewadi, "Backend Engineer", JobStatus.ACTIVE, WorkMode.HYBRID,
                "0", "2", "800000", "1200000", "Build APIs with Java and Spring Boot.",
                Instant.now().minusSeconds(3600));
        seniorJob = job(zetaSoft, hinjewadi, "Senior Data Engineer", JobStatus.ACTIVE, WorkMode.HYBRID,
                "4", "7", "2000000", "3000000", "Own data pipelines and warehouse reliability.",
                Instant.now().minusSeconds(7200));
        frontendJob = job(banerFintech, baner, "Frontend Engineer", JobStatus.ACTIVE, WorkMode.REMOTE,
                "1", "3", "700000", "1000000", "Build React interfaces for fintech products.",
                Instant.now().minusSeconds(1800));
        expiredJob = job(zetaSoft, hinjewadi, "Legacy Role", JobStatus.EXPIRED, WorkMode.ONSITE,
                "0", "1", "400000", "500000", "Old listing that must not be shown.",
                Instant.now().minusSeconds(10800));
        hiddenJob = job(hiddenCorp, hiddenLocation, "Hidden Engineer", JobStatus.ACTIVE, WorkMode.ONSITE,
                "0", "1", "500000", "600000", "Job at an inactive company.",
                Instant.now().minusSeconds(14400));
        oldJob = job(zetaSoft, hinjewadi, "Ancient Engineer", JobStatus.ACTIVE, WorkMode.ONSITE,
                "0", "1", "400000", "500000", "Old posting that must not be shown.",
                Instant.now().minus(Duration.ofDays(30)));

        addSkill(backendJob, "java", SkillRequirementType.REQUIRED);
        addSkill(backendJob, "spring boot", SkillRequirementType.REQUIRED);
        addSkill(backendJob, "sql", SkillRequirementType.PREFERRED);
        addSkill(seniorJob, "python", SkillRequirementType.REQUIRED);
        addSkill(seniorJob, "sql", SkillRequirementType.REQUIRED);
        addSkill(frontendJob, "react", SkillRequirementType.REQUIRED);
        addSkill(frontendJob, "typescript", SkillRequirementType.REQUIRED);
        addSkill(hiddenJob, "java", SkillRequirementType.REQUIRED);
        addSkill(expiredJob, "go", SkillRequirementType.REQUIRED);
        addSkill(hiddenJob, "kotlin", SkillRequirementType.REQUIRED);

        addSource(backendJob, "official_website", "https://zetasoft.example/jobs/backend",
                "https://zetasoft.example/jobs/backend/apply");
        addSource(frontendJob, "linkedin", "https://linkedin.com/jobs/frontend-baner",
                "https://banerfintech.example/careers/frontend");
    }

    @Test
    void searchJobsReturnsOnlyFreshActiveJobsFromActiveCompanies() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/jobs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.path("totalElements").asLong()).isEqualTo(3L);
        JsonNode content = body.path("content");
        assertThat(content).hasSize(3);
        assertThat(titles(content)).containsExactlyInAnyOrder(
                "Backend Engineer", "Senior Data Engineer", "Frontend Engineer");
        for (JsonNode job : content) {
            assertThat(job.path("status").asText()).isEqualTo("ACTIVE");
        }
    }

    @Test
    void jobSummariesIncludeCompanyAndLocation() {
        JsonNode jobs = getJobs("keyword=backend");

        assertThat(jobs).hasSize(1);
        JsonNode job = jobs.get(0);
        assertThat(job.path("company").path("name").asText()).isEqualTo("ZetaSoft");
        assertThat(job.path("company").path("industryName").asText()).isEqualTo("SaaS");
        assertThat(job.path("location").path("area").asText()).isEqualTo("Hinjewadi Phase 2");
        assertThat(job.path("location").path("city").asText()).isEqualTo("Pune");
    }

    @Test
    void skillsEndpointReturnsOnlySkillsUsedByActiveJobsSortedByName() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/skills", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.isArray()).isTrue();

        java.util.List<String> names = new java.util.ArrayList<>();
        body.forEach(node -> names.add(node.path("name").asText()));

        assertThat(names).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(names).contains("Java", "React", "SQL", "Spring Boot");
        assertThat(names).doesNotContain("Go", "Kotlin");
    }

    @Test
    void searchJobsMatchesKeywordAgainstTitleCompanyAndSkillNames() {
        assertThat(titles(getJobs("keyword=backend"))).containsExactly("Backend Engineer");
        assertThat(titles(getJobs("keyword=zetasoft"))).containsExactlyInAnyOrder(
                "Backend Engineer", "Senior Data Engineer");
        assertThat(titles(getJobs("keyword=java"))).containsExactly("Backend Engineer");
        assertThat(titles(getJobs("keyword=react"))).containsExactly("Frontend Engineer");
        assertThat(titles(getJobs("keyword=does-not-exist"))).isEmpty();
    }

    @Test
    void searchJobsFiltersByCompanyLocationIndustryTypeModeFresherAndExperience() {
        assertThat(titles(getJobs("companyId=" + zetaSoft.getId()))).containsExactlyInAnyOrder(
                "Backend Engineer", "Senior Data Engineer");
        assertThat(titles(getJobs("area=baner"))).containsExactly("Frontend Engineer");
        assertThat(titles(getJobs("city=Pune"))).hasSize(3);
        assertThat(titles(getJobs("industry=fintech"))).containsExactly("Frontend Engineer");
        assertThat(titles(getJobs("employmentType=FULL_TIME"))).hasSize(3);
        assertThat(titles(getJobs("workMode=REMOTE"))).containsExactly("Frontend Engineer");
        assertThat(titles(getJobs("fresher=true"))).containsExactlyInAnyOrder(
                "Backend Engineer", "Frontend Engineer");
        assertThat(titles(getJobs("experienceYears=1"))).containsExactlyInAnyOrder(
                "Backend Engineer", "Frontend Engineer");
        assertThat(titles(getJobs("experienceYears=5"))).containsExactly("Senior Data Engineer");
    }

    @Test
    void searchJobsFiltersByOneOrMoreSkillNames() {
        assertThat(titles(getJobs("skill=java"))).containsExactly("Backend Engineer");
        assertThat(titles(getJobs("skill=java&skill=sql"))).containsExactly("Backend Engineer");
        assertThat(titles(getJobs("skill=java&skill=react"))).isEmpty();
    }

    @Test
    void searchJobsSortsByNewestOldestAndSalary() {
        assertThat(titles(getJobs("sort=newest"))).containsExactly(
                "Frontend Engineer", "Backend Engineer", "Senior Data Engineer");
        assertThat(titles(getJobs("sort=oldest"))).containsExactly(
                "Senior Data Engineer", "Backend Engineer", "Frontend Engineer");
        assertThat(titles(getJobs("sort=salary"))).containsExactly(
                "Senior Data Engineer", "Backend Engineer", "Frontend Engineer");
    }

    @Test
    void searchJobsRejectsInvalidParameters() {
        assertInvalid("/api/v1/jobs?sort=bogus");
        assertInvalid("/api/v1/jobs?size=100");
        assertInvalid("/api/v1/jobs?page=-1");
        assertInvalid("/api/v1/jobs?employmentType=BOGUS");
        assertInvalid("/api/v1/jobs?workMode=BOGUS");
        assertInvalid("/api/v1/jobs?companyId=not-a-uuid");
        assertInvalid("/api/v1/jobs?experienceYears=-1");
    }

    @Test
    void jobDetailReturnsCompanyLocationSkillsSourcesAndApplicationLink() {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity("/api/v1/jobs/{id}", JsonNode.class, backendJob.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.path("title").asText()).isEqualTo("Backend Engineer");
        assertThat(body.path("description").asText()).contains("Java and Spring Boot");
        assertThat(body.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(body.path("employmentType").asText()).isEqualTo("FULL_TIME");
        assertThat(body.path("workMode").asText()).isEqualTo("HYBRID");
        assertThat(body.path("fresherEligible").asBoolean()).isTrue();
        assertThat(body.path("postedAt").isNull()).isFalse();
        assertThat(body.path("company").path("name").asText()).isEqualTo("ZetaSoft");
        assertThat(body.path("company").path("industryName").asText()).isEqualTo("SaaS");
        assertThat(body.path("location").path("area").asText()).isEqualTo("Hinjewadi Phase 2");
        assertThat(body.path("location").path("city").asText()).isEqualTo("Pune");
        assertThat(body.path("skills")).hasSize(3);
        assertThat(hasSkill(body.path("skills"), "Java")).isTrue();
        assertThat(hasSkill(body.path("skills"), "Spring Boot")).isTrue();
        assertThat(body.path("sources")).hasSize(1);
        assertThat(body.path("sources").get(0).path("sourceName").asText()).isEqualTo("official_website");
        assertThat(body.path("sources").get(0).path("applicationUrl").asText())
                .isEqualTo("https://zetasoft.example/jobs/backend/apply");
    }

    @Test
    void jobDetailReturns404ForExpiredOldUnknownAndInactiveCompanyJobs() {
        assertNotFound("/api/v1/jobs/" + expiredJob.getId());
        assertNotFound("/api/v1/jobs/" + oldJob.getId());
        assertNotFound("/api/v1/jobs/" + hiddenJob.getId());
        assertNotFound("/api/v1/jobs/" + UUID.randomUUID());
    }

    private JsonNode getJobs(String query) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/jobs?" + query, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().path("content");
    }

    private void assertInvalid(String url) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("error").asText()).isEqualTo("INVALID_PARAMETER");
    }

    private void assertNotFound(String url) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("error").asText()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    private java.util.List<String> titles(JsonNode content) {
        java.util.List<String> titles = new java.util.ArrayList<>();
        for (JsonNode job : content) {
            titles.add(job.path("title").asText());
        }
        return titles;
    }

    private boolean hasSkill(JsonNode skills, String name) {
        for (JsonNode skill : skills) {
            if (name.equals(skill.path("skillName").asText())) {
                return true;
            }
        }
        return false;
    }

    private Company company(String name, String slug, String industrySlug, RecordStatus recordStatus) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setNormalizedName(name.toLowerCase());
        company.setSlug(slug);
        company.setDescription("Test company " + name);
        company.setCompanyType(CompanyType.STARTUP);
        company.setVerificationStatus(VerificationStatus.VERIFIED);
        company.setRecordStatus(recordStatus);
        company.setIndustry(industryRepository.findBySlug(industrySlug).orElseThrow());
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
        location.setLocation(point(73.7275, 18.5913));
        location.setGeocodeStatus(GeocodeStatus.VERIFIED);
        location.setHeadquarters(true);
        return locationRepository.save(location);
    }

    private Job job(Company company, CompanyLocation location, String title, JobStatus status, WorkMode workMode,
                    String minYears, String maxYears, String salaryMin, String salaryMax, String description,
                    Instant firstPublishedAt) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setCompany(company);
        job.setCompanyLocation(location);
        job.setTitle(title);
        job.setNormalizedTitle(title.toLowerCase());
        job.setDescription(description);
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setWorkMode(workMode);
        job.setExperienceMinYears(new BigDecimal(minYears));
        job.setExperienceMaxYears(new BigDecimal(maxYears));
        job.setSalaryMin(new BigDecimal(salaryMin));
        job.setSalaryMax(new BigDecimal(salaryMax));
        job.setSalaryCurrency("INR");
        job.setSalaryPeriod("YEAR");
        job.setStatus(status);
        job.setFirstPublishedAt(firstPublishedAt);
        job.setLastVerifiedAt(Instant.now());
        return jobRepository.save(job);
    }

    private void addSkill(Job job, String normalizedSkill, SkillRequirementType requirementType) {
        JobSkill jobSkill = new JobSkill();
        JobSkillId id = new JobSkillId(job.getId(), skillRepository.findByNormalizedName(normalizedSkill)
                .orElseThrow().getId());
        jobSkill.setId(id);
        jobSkill.setJob(job);
        jobSkill.setSkill(skillRepository.findByNormalizedName(normalizedSkill).orElseThrow());
        jobSkill.setRequirementType(requirementType);
        jobSkillRepository.save(jobSkill);
    }

    private void addSource(Job job, String sourceName, String sourceUrl, String applicationUrl) {
        JobSource source = new JobSource();
        source.setId(UUID.randomUUID());
        source.setJob(job);
        source.setSourceName(sourceName);
        source.setSourceUrl(sourceUrl);
        source.setApplicationUrl(applicationUrl);
        source.setSourceStatus("ACTIVE");
        source.setFirstSeenAt(Instant.now().minusSeconds(3600));
        source.setLastSeenAt(Instant.now());
        source.setLastCheckedAt(Instant.now());
        jobSourceRepository.save(source);
    }

    private Point point(double longitude, double latitude) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }
}
