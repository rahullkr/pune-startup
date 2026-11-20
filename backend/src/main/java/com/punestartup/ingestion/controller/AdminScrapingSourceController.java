package com.punestartup.ingestion.controller;

import com.punestartup.common.web.PageResponse;
import com.punestartup.ingestion.dto.CreateScrapingSourceRequest;
import com.punestartup.ingestion.dto.ScrapingRunResponse;
import com.punestartup.ingestion.dto.ScrapingSourceResponse;
import com.punestartup.ingestion.dto.UpdateScrapingSourceRequest;
import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ScrapingSourceType;
import com.punestartup.ingestion.service.ScrapingRunService;
import com.punestartup.ingestion.service.ScrapingSourceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/scraping-sources")
public class AdminScrapingSourceController {

    private final ScrapingSourceService sourceService;
    private final ScrapingRunService runService;

    public AdminScrapingSourceController(ScrapingSourceService sourceService, ScrapingRunService runService) {
        this.sourceService = sourceService;
        this.runService = runService;
    }

    @GetMapping
    public PageResponse<ScrapingSourceResponse> listSources(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) PermissionReviewStatus permissionStatus,
            @RequestParam(required = false) ScrapingSourceType sourceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(sourceService.listSources(enabled, permissionStatus, sourceType, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ScrapingSourceResponse getSource(@PathVariable UUID id) {
        return sourceService.getSource(id);
    }

    @PostMapping
    public ResponseEntity<ScrapingSourceResponse> createSource(
            @Valid @RequestBody CreateScrapingSourceRequest request) {
        ScrapingSourceResponse created = sourceService.createSource(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/scraping-sources/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public ScrapingSourceResponse updateSource(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateScrapingSourceRequest request) {
        return sourceService.updateSource(id, request);
    }

    @PostMapping("/{id}/runs")
    public ResponseEntity<ScrapingRunResponse> startRun(@PathVariable UUID id) {
        ScrapingRunResponse run = runService.startRun(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/v1/admin/scraping-runs/" + run.id()))
                .body(run);
    }
}
