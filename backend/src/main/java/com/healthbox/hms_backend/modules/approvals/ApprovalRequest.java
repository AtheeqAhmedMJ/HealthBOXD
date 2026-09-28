package com.healthbox.hms_backend.modules.approvals;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_requests")
@Getter
@Setter
public class ApprovalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_type", nullable = false, length = 60)
    private String requestType;

    @Column(name = "hospital_id")
    private Long hospitalId;

    @Column(name = "requested_by", length = 30)
    private String requestedBy;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(length = 1000)
    private String details;

    @Column(name = "reviewed_by", length = 30)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
