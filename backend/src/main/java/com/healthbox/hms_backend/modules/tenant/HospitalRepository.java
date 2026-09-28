package com.healthbox.hms_backend.modules.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByCode(String code);
    boolean existsByCode(String code);
    java.util.List<Hospital> findAllByOrderByCreatedAtDesc();
}
