package com.healthbox.hms_backend.modules.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospitals")
@Getter
@Setter
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code; // short tenant slug, e.g. "APOLLO01"

    @Column(length = 255)
    private String location;

    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    @Column(name = "approval_reason", length = 500)
    private String approvalReason;

    private LocalDateTime approvedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
