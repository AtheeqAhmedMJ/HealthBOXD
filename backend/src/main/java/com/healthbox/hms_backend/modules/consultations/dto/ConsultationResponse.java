package com.healthbox.hms_backend.modules.consultations.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConsultationResponse(
        Long id,
        String patientPhno,
        String doctorPhno,
        Long appointmentId,
        String status,
        String patientType,
        Map<String, Object> inpatientDetails,
        List<Map<String, Object>> injections,
        List<Map<String, Object>> medicines,
        String symptoms,
        String diagnosis,
        String clinicalNotes,
        String generalNotes,
        String bp,
        String grbs,
        String spo2,
        String temperature,
        LocalDate nextVisitDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime finalizedAt
) {}
