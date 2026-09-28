package com.healthbox.hms_backend.modules.notifications;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_phno", nullable = false)
    private String patientPhno;
    @Column(nullable = false)
    private String channel;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private String status = "PENDING";
    @Column(nullable = false)
    private int attempts;
    private String providerReference;
    private String failureReason;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime sentAt;
    private LocalDateTime nextAttemptAt;
}
