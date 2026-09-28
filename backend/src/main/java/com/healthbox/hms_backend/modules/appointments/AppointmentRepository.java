package com.healthbox.hms_backend.modules.appointments;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientPhno(String phno);
    List<Appointment> findByHospitalId(Long hospitalId);
    List<Appointment> findByHospitalIdAndDoctorPhno(Long hospitalId, String doctorPhno);
    boolean existsByPatientPhnoAndDate(String patientPhno, LocalDate date);
    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
    boolean existsByHospitalIdAndPatientPhnoAndDateAndStatusNotIn(Long hospitalId, String patientPhno, LocalDate date, List<String> statuses);
}
