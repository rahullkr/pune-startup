package com.punestartup.job.dto;

import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.WorkMode;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public record JobSearchRequest(
        String keyword,
        UUID companyId,
        String city,
        String area,
        String industry,
        EmploymentType employmentType,
        WorkMode workMode,
        boolean fresherOnly,
        BigDecimal experienceYears,
        List<String> skills,
        int page,
        int size,
        String sort) {

    public static JobSearchRequest of(String keyword, String companyId, String city, String area, String industry,
                                      String employmentType, String workMode, Boolean fresher,
                                      BigDecimal experienceYears, List<String> skills, Integer page, Integer size,
                                      String sort) {
        if (experienceYears != null && experienceYears.signum() < 0) {
            throw InvalidParameterException.of("experienceYears", "must be 0 or greater");
        }
        return new JobSearchRequest(
                keyword,
                parseUuid(companyId),
                city,
                area,
                industry,
                parseEnum(EmploymentType.class, employmentType, "employmentType"),
                parseEnum(WorkMode.class, workMode, "workMode"),
                Boolean.TRUE.equals(fresher),
                experienceYears,
                skills == null ? List.of() : skills.stream()
                        .filter(skill -> skill != null && !skill.isBlank())
                        .toList(),
                page == null ? 0 : page,
                size == null ? 20 : size,
                sort == null || sort.isBlank() ? "newest" : sort.toLowerCase(Locale.ROOT));
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw InvalidParameterException.of("companyId", "must be a valid UUID");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String parameter) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            String allowed = Arrays.stream(type.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw InvalidParameterException.of(parameter, "allowed values are " + allowed);
        }
    }
}
