package com.healthbox.hms_backend.modules.notifications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnMissingBean(SmsProvider.class)
public class DevelopmentSmsProvider implements SmsProvider {
    private final Map<String, String> lastMessages = new ConcurrentHashMap<>();

    @Override
    public SmsResult send(String phoneNumber, String message) {
        lastMessages.put(phoneNumber, message);
        return new SmsResult(false, null, "SMS provider is not configured");
    }

    public String lastMessage(String phoneNumber) {
        return lastMessages.get(phoneNumber);
    }
}
