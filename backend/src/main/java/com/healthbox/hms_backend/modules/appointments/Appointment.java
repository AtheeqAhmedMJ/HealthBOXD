package com.healthbox.hms_backend.modules.appointments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String patientPhno;

    private String patientName; // display name before a paid consultation creates a real Patient profile

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "appointment_time")
    private LocalTime appointmentTime;

    @Column(length = 500)
    private String reason;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "doctor_phno")
    private String doctorPhno; // ownership attribute for ABAC

    @Column(nullable = false)
    private String status = "BOOKED";

    @Column(name = "appointment_type", nullable = false)
    private String appointmentType = "PRE_BOOKED";

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
