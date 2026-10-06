package com.healthbox.hms_backend.modules.fees;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_fee_brackets")
@Getter
@Setter
public class ServiceFeeBracket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hospital_id")
    private Long hospitalId;

    @Column(name = "min_amount_paise", nullable = false)
    private Long minAmountPaise;

    @Column(name = "max_amount_paise")
    private Long maxAmountPaise;

    @Column(name = "fee_type", nullable = false, length = 20)
    private String feeType;

    @Column(name = "fee_value", nullable = false)
    private Long feeValue;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
