package com.healthbox.hms_backend.modules.payments.dto;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String razorpayOrderId,
        String razorpayPaymentId,
        Long appointmentId,
        Long consultationId,
        Long prescriptionId,
        long amountPaise,
        String status,
        String transferStatus,
        String transferId,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {}
