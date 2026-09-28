package com.healthbox.hms_backend.modules.notifications;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final SmsProvider smsProvider;

    public NotificationService(NotificationRepository repository, SmsProvider smsProvider) {
        this.repository = repository;
        this.smsProvider = smsProvider;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification queuePrescriptionReady(String patientPhno, Long prescriptionId) {
        Notification notification = new Notification();
        notification.setPatientPhno(patientPhno);
        notification.setChannel("SMS");
        notification.setEventType("PRESCRIPTION_READY");
        notification.setRecipient(patientPhno);
        notification.setMessage("Your HealthBoxD medical record is ready. Sign in with your verified mobile number to view it. Reference: " + prescriptionId);
        notification.setNextAttemptAt(LocalDateTime.now());
        return repository.save(notification);
    }

    @Scheduled(fixedDelayString = "${notifications.worker-delay-ms:5000}")
    @Transactional
    public void processPending() {
        List<Notification> jobs = repository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                "PENDING", LocalDateTime.now());
        for (Notification notification : jobs) process(notification);
    }

    private void process(Notification notification) {
        notification.setStatus("PROCESSING");
        notification.setAttempts(notification.getAttempts() + 1);
        SmsResult result = smsProvider.send(notification.getRecipient(), notification.getMessage());
        if (result.delivered()) {
            notification.setStatus("SENT");
            notification.setProviderReference(result.providerReference());
            notification.setSentAt(LocalDateTime.now());
            notification.setFailureReason(null);
        } else if (notification.getAttempts() >= 5) {
            notification.setStatus("FAILED");
            notification.setFailureReason(result.failureReason());
        } else {
            notification.setStatus("PENDING");
            notification.setFailureReason(result.failureReason());
            notification.setNextAttemptAt(LocalDateTime.now().plusMinutes(notification.getAttempts()));
        }
        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> forPatient(String phoneNumber) {
        return repository.findByPatientPhnoOrderByCreatedAtDesc(phoneNumber);
    }
}
