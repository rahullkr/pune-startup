package com.punestartup.company.service;

import com.punestartup.company.dto.IndustryResponse;
import com.punestartup.company.repository.IndustryRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndustryService {

    private final IndustryRepository industryRepository;

    public IndustryService(IndustryRepository industryRepository) {
        this.industryRepository = industryRepository;
    }

    @Transactional(readOnly = true)
    public List<IndustryResponse> listIndustries() {
        return industryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(IndustryResponse::from)
                .toList();
    }
}
