package com.healthbox.hms_backend.modules.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorClinicMembershipRepository extends JpaRepository<DoctorClinicMembership, Long> {
    List<DoctorClinicMembership> findByDoctorPhnoAndActiveTrue(String doctorPhno);
    Optional<DoctorClinicMembership> findByDoctorPhnoAndHospitalIdAndActiveTrue(String doctorPhno, Long hospitalId);
    boolean existsByDoctorPhnoAndHospitalId(String doctorPhno, Long hospitalId);
}
