package com.healthbox.hms_backend.modules.notifications;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public List<Notification> mine() {
        if (currentUser.get().getRole() != Role.PATIENT) throw new AccessDeniedException("Patient access required");
        return service.forPatient(currentUser.get().getPhno());
    }
}
