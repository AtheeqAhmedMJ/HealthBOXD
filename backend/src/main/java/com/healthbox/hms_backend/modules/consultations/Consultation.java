package com.healthbox.hms_backend.modules.consultations;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "consultations")
@Getter
@Setter
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_phno", nullable = false, length = 20)
    private String patientPhno;

    @Column(name = "doctor_phno", nullable = false, length = 20)
    private String doctorPhno;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(nullable = false)
    private String status = "CREATED";

    @Column(name = "patient_type", nullable = false)
    private String patientType = "OP";

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> inpatientDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> injections;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> medicines;

    @Version
    private Long version;

    private String symptoms;
    private String diagnosis;
    private String clinicalNotes;
    private String generalNotes;
    private String bp;
    private String grbs;
    private String spo2;
    private String temperature;
    private LocalDate nextVisitDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime finalizedAt;
}
