package com.punestartup.ingestion.controller;

import com.punestartup.common.web.PageResponse;
import com.punestartup.ingestion.dto.ScrapedRecordDetailResponse;
import com.punestartup.ingestion.dto.ScrapedRecordSummaryResponse;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.service.ScrapedRecordService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/scraped-records")
public class AdminScrapedRecordController {

    private final ScrapedRecordService recordService;

    public AdminScrapedRecordController(ScrapedRecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public PageResponse<ScrapedRecordSummaryResponse> listRecords(
            @RequestParam(required = false) UUID runId,
            @RequestParam(required = false) UUID sourceId,
            @RequestParam(required = false) ScrapedRecordStatus processingStatus,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(recordService.listRecords(runId, sourceId, processingStatus, page, size));
    }

    @GetMapping("/{id}")
    public ScrapedRecordDetailResponse getRecord(@PathVariable UUID id) {
        return recordService.getRecord(id);
    }

    @PostMapping("/{id}/publish")
    public ScrapedRecordDetailResponse publish(@PathVariable UUID id) {
        return recordService.publish(id);
    }

    @PostMapping("/{id}/reject")
    public ScrapedRecordDetailResponse reject(@PathVariable UUID id) {
        return recordService.reject(id);
    }
}
