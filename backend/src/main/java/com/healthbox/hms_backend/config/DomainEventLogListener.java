package com.healthbox.hms_backend.config;

import com.healthbox.hms_backend.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainEventLogListener {
    private static final Logger log = LoggerFactory.getLogger(DomainEventLogListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        log.debug("Domain event {} at {}", event.getClass().getSimpleName(), event.occurredAt());
    }
}
