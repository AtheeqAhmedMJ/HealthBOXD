package com.healthbox.hms_backend.shared.events;

import java.time.Instant;

public record PaymentCompleted(Long paymentOrderId, Long hospitalId, String patientPhno, Instant occurredAt) implements DomainEvent {
    public PaymentCompleted(Long paymentOrderId, Long hospitalId, String patientPhno) {
        this(paymentOrderId, hospitalId, patientPhno, Instant.now());
    }
}
