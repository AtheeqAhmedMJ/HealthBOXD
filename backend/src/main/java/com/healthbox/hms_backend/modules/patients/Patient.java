package com.healthbox.hms_backend.modules.patients;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Getter
@Setter
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "phno", nullable = false, unique = true, length = 20)
    private String phno;

    @Column(nullable = false)
    private String name;

    private Integer age;
    private LocalDate dob;
    private String gender;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "assigned_doctor_phno")
    private String assignedDoctorPhno; // ownership attribute for ABAC

    private LocalDateTime createdAt = LocalDateTime.now();
}
