package com.punestartup.company.repository;

import com.punestartup.company.entity.Industry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndustryRepository extends JpaRepository<Industry, UUID> {

    Optional<Industry> findBySlug(String slug);
}
