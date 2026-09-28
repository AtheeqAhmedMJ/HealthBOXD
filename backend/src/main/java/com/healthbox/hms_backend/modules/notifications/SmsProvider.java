package com.healthbox.hms_backend.modules.notifications;

public interface SmsProvider {
    SmsResult send(String phoneNumber, String message);
}
