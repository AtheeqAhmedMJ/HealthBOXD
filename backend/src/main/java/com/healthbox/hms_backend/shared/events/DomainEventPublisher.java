package com.healthbox.hms_backend.shared.events;

public interface DomainEventPublisher {
    void publish(DomainEvent event);

    static DomainEventPublisher noop() {
        return event -> { };
    }
}
