package com.healthbox.hms_backend.modules.appointments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientPhno(String phno);
    List<Appointment> findByHospitalId(Long hospitalId);
    List<Appointment> findByHospitalIdAndDoctorPhno(Long hospitalId, String doctorPhno);
    boolean existsByPatientPhnoAndDate(String patientPhno, LocalDate date);
    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
    boolean existsByHospitalIdAndPatientPhnoAndDateAndStatusNotIn(Long hospitalId, String patientPhno, LocalDate date, List<String> statuses);
    long countByHospitalId(Long hospitalId);
    long countByHospitalIdAndDate(Long hospitalId, LocalDate date);
    long countByHospitalIdAndDateBetween(Long hospitalId, LocalDate from, LocalDate to);
    @Query("select a.patientPhno from Appointment a where a.hospitalId = :hospitalId group by a.patientPhno having count(a) > 1")
    Set<String> findRecurringPatients(@Param("hospitalId") Long hospitalId);
    @Query("select a.date, count(a) from Appointment a where a.hospitalId = :hospitalId and a.date between :from and :to group by a.date order by a.date")
    List<Object[]> countByDateBetween(@Param("hospitalId") Long hospitalId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
