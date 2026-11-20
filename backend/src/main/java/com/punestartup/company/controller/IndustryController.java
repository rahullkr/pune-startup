package com.punestartup.company.controller;

import com.punestartup.company.dto.IndustryResponse;
import com.punestartup.company.service.IndustryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class IndustryController {

    private final IndustryService industryService;

    public IndustryController(IndustryService industryService) {
        this.industryService = industryService;
    }

    @GetMapping("/industries")
    public List<IndustryResponse> listIndustries() {
        return industryService.listIndustries();
    }
}
