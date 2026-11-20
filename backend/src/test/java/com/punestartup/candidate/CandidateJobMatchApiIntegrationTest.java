package com.punestartup.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.punestartup.auth.repository.AuthSessionRepository;
import com.punestartup.auth.repository.UserRepository;
import com.punestartup.auth.repository.UserRoleRepository;
import com.punestartup.candidate.repository.CandidateProfileRepository;
import com.punestartup.candidate.repository.CandidateSkillRepository;
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
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.SkillRequirementType;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSkillRepository;
import com.punestartup.skill.entity.Skill;
import com.punestartup.skill.repository.SkillRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class CandidateJobMatchApiIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String PROFILE_URL = "/api/v1/candidates/me";
    private static final String MATCHES_URL = "/api/v1/candidates/me/job-matches";

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
    private JobSkillRepository jobSkillRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private CompanyLocationRepository locationRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private IndustryRepository industryRepository;
    @Autowired
    private SkillRepository skillRepository;

    private String token;
    private Company company;

    @BeforeEach
    void cleanAndSeed() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        jobSkillRepository.deleteAll();
        jobRepository.deleteAll();
        locationRepository.deleteAll();
        companyRepository.deleteAll();
        sessionRepository.deleteAll();
        candidateSkillRepository.deleteAll();
        profileRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        token = register("matcher@example.com", "Match Candidate");
        company = company("ZetaSoft", "zetasoft", RecordStatus.ACTIVE);
    }

    @Test
    void ranksMatchesAndExplainsSkillSignals() {
        assertThat(get(MATCHES_URL, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode empty = get(MATCHES_URL, token).getBody();
        assertThat(empty.path("totalElements").asLong()).isZero();

        updateSkills("Java", "Spring Boot");

        createJob("Platform Engineer", JobStatus.ACTIVE, company,
                skillSpec("Java", SkillRequirementType.REQUIRED),
                skillSpec("Spring Boot", SkillRequirementType.REQUIRED),
                skillSpec("SQL", SkillRequirementType.PREFERRED));
        createJob("Java Developer", JobStatus.ACTIVE, company,
                skillSpec("Java", SkillRequirementType.REQUIRED),
                skillSpec("React", SkillRequirementType.PREFERRED));
        createJob("Python Engineer", JobStatus.ACTIVE, company,
                skillSpec("Python", SkillRequirementType.REQUIRED));
        createJob("Old Java Role", JobStatus.EXPIRED, company,
                skillSpec("Java", SkillRequirementType.REQUIRED));
        createJob("No Skill Role", JobStatus.ACTIVE, company);

        Company inactive = company("Hidden Corp", "hidden-corp", RecordStatus.INACTIVE);
        createJob("Inactive Java Role", JobStatus.ACTIVE, inactive,
                skillSpec("Java", SkillRequirementType.REQUIRED));

        JsonNode matches = get(MATCHES_URL, token).getBody();
        assertThat(matches.path("totalElements").asLong()).isEqualTo(2);
        JsonNode first = matches.path("content").get(0);
        JsonNode second = matches.path("content").get(1);

        assertThat(first.path("job").path("title").asText()).isEqualTo("Platform Engineer");
        assertThat(first.path("matchScore").asInt()).isEqualTo(80);
        assertThat(first.path("matchedSkillCount").asInt()).isEqualTo(2);
        assertThat(first.path("requiredSkillCount").asInt()).isEqualTo(2);
        assertThat(first.path("matchedRequiredSkillCount").asInt()).isEqualTo(2);
        assertThat(first.path("preferredSkillCount").asInt()).isEqualTo(1);
        assertThat(first.path("matchedPreferredSkillCount").asInt()).isZero();
        assertThat(first.path("matchedSkills")).hasSize(2);
        assertThat(first.path("missingSkills")).hasSize(1);
        assertThat(first.path("missingSkills").get(0).path("skillName").asText()).isEqualTo("SQL");
        assertThat(first.path("signals")).hasSize(2);
        assertThat(hasSignal(first.path("signals"), "REQUIRED_SKILLS_MATCHED",
                "Matches 2 of 2 required skills: Java, Spring Boot")).isTrue();
        assertThat(hasSignal(first.path("signals"), "PREFERRED_SKILLS_MISSING",
                "Missing 1 of 1 preferred skills: SQL")).isTrue();

        assertThat(second.path("job").path("title").asText()).isEqualTo("Java Developer");
        assertThat(second.path("matchScore").asInt()).isEqualTo(67);
        assertThat(second.path("matchedSkills").get(0).path("skillName").asText()).isEqualTo("Java");
        assertThat(second.path("missingSkills").get(0).path("skillName").asText()).isEqualTo("React");
        assertThat(hasSignal(second.path("signals"), "PREFERRED_SKILLS_MISSING",
                "Missing 1 of 1 preferred skills: React")).isTrue();

        JsonNode firstPage = get(MATCHES_URL + "?page=0&size=1", token).getBody();
        assertThat(firstPage.path("totalElements").asLong()).isEqualTo(2);
        assertThat(firstPage.path("content")).hasSize(1);
        assertThat(firstPage.path("content").get(0).path("job").path("title").asText())
                .isEqualTo("Platform Engineer");
        assertThat(firstPage.path("hasNext").asBoolean()).isTrue();

        assertThat(get(MATCHES_URL + "?size=100", token).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get(MATCHES_URL + "?page=-1", token).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private boolean hasSignal(JsonNode signals, String code, String message) {
        for (JsonNode signal : signals) {
            if (signal.path("code").asText().equals(code)
                    && signal.path("message").asText().equals(message)) {
                return true;
            }
        }
        return false;
    }

    private void updateSkills(String... names) {
        List<Map<String, Object>> skills = java.util.Arrays.stream(names)
                .map(name -> Map.<String, Object>of("name", name, "proficiency", "ADVANCED"))
                .toList();
        ResponseEntity<JsonNode> response = patch(PROFILE_URL, Map.of("skills", skills), token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("skills")).hasSize(names.length);
    }

    private SkillSpec skillSpec(String name, SkillRequirementType requirementType) {
        return new SkillSpec(name, requirementType);
    }

    private Job createJob(String title, JobStatus status, Company company, SkillSpec... skillSpecs) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setCompany(company);
        job.setCompanyLocation(locationFor(company));
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
        job.setFirstPublishedAt(Instant.now().minusSeconds(3600));
        job.setLastVerifiedAt(Instant.now());
        job = jobRepository.save(job);

        for (SkillSpec spec : skillSpecs) {
            Skill skill = skillRepository.findByNormalizedName(spec.name().toLowerCase()).orElseThrow();
            JobSkill jobSkill = new JobSkill();
            jobSkill.setId(new JobSkillId(job.getId(), skill.getId()));
            jobSkill.setJob(job);
            jobSkill.setSkill(skill);
            jobSkill.setRequirementType(spec.requirementType());
            jobSkillRepository.save(jobSkill);
        }
        return job;
    }

    private CompanyLocation locationFor(Company target) {
        List<CompanyLocation> existing = locationRepository.findByCompanyIdOrderByHeadquartersDesc(target.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return location(target, "Baner", "Pune");
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

    private ResponseEntity<JsonNode> patch(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.PATCH, entity(body, token), JsonNode.class);
    }

    private HttpEntity<Object> entity(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    private record SkillSpec(String name, SkillRequirementType requirementType) {
    }
}
