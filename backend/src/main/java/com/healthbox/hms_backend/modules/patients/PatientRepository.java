package com.healthbox.hms_backend.modules.patients;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    java.util.Optional<Patient> findByPhno(String phno);
    List<Patient> findByHospitalId(Long hospitalId);
    List<Patient> findByHospitalIdAndAssignedDoctorPhno(Long hospitalId, String doctorPhno);
}
