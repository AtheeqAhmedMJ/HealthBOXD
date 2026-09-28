package com.healthbox.hms_backend.modules.medications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    Page<Medication> findByActiveTrueAndNameContainingIgnoreCaseOrderByName(String search, Pageable pageable);
}
