package com.healthbox.hms_backend.modules.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    List<User> findByHospitalId(Long hospitalId);
    List<User> findByHospitalIdAndRole(Long hospitalId, Role role);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    boolean existsByRole(Role role);
}
