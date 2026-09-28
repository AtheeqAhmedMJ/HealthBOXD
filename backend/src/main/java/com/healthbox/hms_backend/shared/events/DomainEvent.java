package com.healthbox.hms_backend.shared.events;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
