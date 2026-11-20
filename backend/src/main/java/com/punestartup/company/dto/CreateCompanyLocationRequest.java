package com.punestartup.company.dto;

import com.punestartup.company.entity.GeocodeStatus;
import com.punestartup.company.entity.LocationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyLocationRequest(
        @Size(max = 150) String locationName,
        @Size(max = 2000) String addressLine,
        @Size(max = 120) String area,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Size(min = 2, max = 2)
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a two-letter country code")
        String countryCode,
        @Size(max = 15) String postalCode,
        LocationType locationType,
        GeocodeStatus geocodeStatus,
        Boolean headquarters,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}
