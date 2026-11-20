package com.punestartup.company.dto;

import com.punestartup.company.entity.Industry;
import java.util.UUID;

public record IndustryResponse(UUID id, String name, String slug) {

    public static IndustryResponse from(Industry industry) {
        return new IndustryResponse(industry.getId(), industry.getName(), industry.getSlug());
    }
}
