package com.punestartup.candidate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FollowedCompanyId implements Serializable {

    @Column(name = "candidate_profile_id", nullable = false)
    private UUID candidateProfileId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;
}
