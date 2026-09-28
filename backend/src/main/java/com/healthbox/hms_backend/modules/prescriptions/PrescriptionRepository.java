package com.healthbox.hms_backend.modules.prescriptions;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientPhno(String phno);
    List<Prescription> findByPatientPhnoAndStatusOrderByCreatedAtDesc(String phno, String status);
    List<Prescription> findByAppointmentId(Long appointmentId);
    List<Prescription> findByHospitalId(Long hospitalId);
    List<Prescription> findByHospitalIdAndDoctorPhno(Long hospitalId, String doctorPhno);
    Optional<Prescription> findByConsultationId(Long consultationId);
}
