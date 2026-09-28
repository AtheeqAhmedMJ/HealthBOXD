package com.healthbox.hms_backend.modules.consultations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByDoctorPhnoAndStatusOrderByCreatedAtDesc(String doctorPhno, String status);
    Page<Consultation> findByPatientPhnoAndStatusOrderByCreatedAtDesc(String patientPhno, String status, Pageable pageable);
    List<Consultation> findByPatientPhnoAndStatusOrderByCreatedAtDesc(String patientPhno, String status);
    Optional<Consultation> findByAppointmentId(Long appointmentId);
}
