package com.healthbox.hms_backend.modules.notifications;

public record SmsResult(boolean delivered, String providerReference, String failureReason) {}
