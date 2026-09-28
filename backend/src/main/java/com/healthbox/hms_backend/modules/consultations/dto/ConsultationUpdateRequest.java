package com.healthbox.hms_backend.modules.consultations.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
public class ConsultationUpdateRequest {
    @Pattern(regexp = "OP|IP", message = "Patient type must be OP or IP")
    private String patientType;
    private Map<String, Object> inpatientDetails;
    private List<Map<String, Object>> injections;
    private List<Map<String, Object>> medicines;
    private String symptoms;
    private String diagnosis;
    private String clinicalNotes;
    private String generalNotes;
    private String bp;
    private String grbs;
    private String spo2;
    private String temperature;
    private LocalDate nextVisitDate;
}
