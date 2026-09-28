package com.healthbox.hms_backend.modules.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_clinic_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_phno", "hospital_id"}))
@Getter
@Setter
public class DoctorClinicMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_phno", nullable = false, length = 30)
    private String doctorPhno;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
