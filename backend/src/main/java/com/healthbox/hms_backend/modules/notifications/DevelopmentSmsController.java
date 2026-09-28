package com.healthbox.hms_backend.modules.notifications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/dev")
@ConditionalOnProperty(name = "sms.dev-inspector-enabled", havingValue = "true")
public class DevelopmentSmsController {
    private final DevelopmentSmsProvider provider;

    public DevelopmentSmsController(DevelopmentSmsProvider provider) {
        this.provider = provider;
    }

    @GetMapping("/otp/{phoneNumber}")
    public Map<String, String> otp(@PathVariable String phoneNumber) {
        String message = provider.lastMessage(phoneNumber);
        if (message == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "No development OTP found");
        return Map.of("message", message);
    }
}