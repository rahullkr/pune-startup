package com.punestartup.company.dto;

public record CompanySearchRequest(
        String keyword,
        String city,
        String area,
        String industry,
        boolean fresherOnly,
        int page,
        int size,
        String sort) {

    public static CompanySearchRequest of(String keyword, String city, String area, String industry,
                                          String roleType, Integer page, Integer size, String sort) {
        boolean fresherOnly = "FRESHER".equalsIgnoreCase(roleType);
        return new CompanySearchRequest(
                keyword,
                city,
                area,
                industry,
                fresherOnly,
                page == null ? 0 : page,
                size == null ? 20 : size,
                sort == null || sort.isBlank() ? "name" : sort.toLowerCase());
    }
}
