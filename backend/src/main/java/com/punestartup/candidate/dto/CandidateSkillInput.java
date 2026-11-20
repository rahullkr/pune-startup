package com.punestartup.candidate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CandidateSkillInput(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) String proficiency,
        @DecimalMin(value = "0.0", message = "must be non-negative")
        @Digits(integer = 3, fraction = 1) BigDecimal yearsExperience) {
}
