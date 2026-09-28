package com.healthbox.hms_backend.modules.prescriptions;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "prescription_medications")
@Getter
@Setter
public class PrescriptionMedication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "medication_type", nullable = false, length = 20)
    private String medicationType;

    @Column(name = "medication_name", nullable = false, length = 150)
    private String medicationName;

    @Column(nullable = false, length = 50)
    private String dosage;

    @Column(nullable = false, length = 30)
    private String quantity;

    @Column(nullable = false, length = 50)
    private String duration;

    @Column(name = "food_instruction", length = 50)
    private String foodInstruction;

    @Column(name = "additional_instructions", length = 500)
    private String additionalInstructions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
