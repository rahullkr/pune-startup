package com.punestartup.auth.repository;

import com.punestartup.auth.entity.UserRole;
import com.punestartup.auth.entity.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser_Id(UUID userId);
}
