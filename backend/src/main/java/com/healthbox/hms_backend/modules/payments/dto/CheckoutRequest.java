package com.healthbox.hms_backend.modules.payments.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CheckoutRequest {
    private String idempotencyKey;
    // identity
    private String patientPhno;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private Long appointmentId;
    private Long consultationId;
    private Long prescriptionId;

    // charges: either pick from the admin's rate card, or supply a one-off amount
    private List<Long> chargeItemIds;
    private Long customAmountPaise;

    // clinical draft — only persisted as a Prescription once payment succeeds
    private String symptoms;
    private String bp;
    private String spo2;
    private String grbs;
    private String temp;
    private List<Map<String, Object>> medicines;
    private String remarks;
}
