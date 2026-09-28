package com.healthbox.hms_backend.shared.events;

import java.time.Instant;

public record AppointmentCreated(Long appointmentId, Long hospitalId, String patientPhno, Instant occurredAt) implements DomainEvent {
    public AppointmentCreated(Long appointmentId, Long hospitalId, String patientPhno) {
        this(appointmentId, hospitalId, patientPhno, Instant.now());
    }
}
