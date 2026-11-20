package com.punestartup.ingestion.controller;

import com.punestartup.common.web.PageResponse;
import com.punestartup.ingestion.dto.ScrapedRecordSummaryResponse;
import com.punestartup.ingestion.dto.ScrapingRunResponse;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import com.punestartup.ingestion.service.ScrapedRecordService;
import com.punestartup.ingestion.service.ScrapingRunService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/scraping-runs")
public class AdminScrapingRunController {

    private final ScrapingRunService runService;
    private final ScrapedRecordService recordService;

    public AdminScrapingRunController(ScrapingRunService runService, ScrapedRecordService recordService) {
        this.runService = runService;
        this.recordService = recordService;
    }

    @GetMapping
    public PageResponse<ScrapingRunResponse> listRuns(
            @RequestParam(required = false) UUID sourceId,
            @RequestParam(required = false) ScrapingRunStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(runService.listRuns(sourceId, status, page, size));
    }

    @GetMapping("/{id}")
    public ScrapingRunResponse getRun(@PathVariable UUID id) {
        return runService.getRun(id);
    }

    @GetMapping("/{id}/errors")
    public PageResponse<ScrapedRecordSummaryResponse> listRunErrors(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        runService.getRun(id);
        return PageResponse.from(recordService.listRecords(id, null, ScrapedRecordStatus.FAILED, page, size));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ScrapingRunResponse> retryRun(@PathVariable UUID id) {
        ScrapingRunResponse run = runService.retryRun(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/v1/admin/scraping-runs/" + run.id()))
                .body(run);
    }
}
