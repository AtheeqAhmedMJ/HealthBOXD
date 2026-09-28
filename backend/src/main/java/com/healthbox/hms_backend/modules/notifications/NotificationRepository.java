package com.healthbox.hms_backend.modules.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status, LocalDateTime now);
    List<Notification> findByPatientPhnoOrderByCreatedAtDesc(String patientPhno);
}
