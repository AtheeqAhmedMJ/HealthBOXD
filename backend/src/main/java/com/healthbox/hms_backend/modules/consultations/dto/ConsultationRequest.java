package com.healthbox.hms_backend.modules.consultations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ConsultationRequest {
    @NotBlank
    private String patientPhno;
    private Long appointmentId;
    @Pattern(regexp = "OP|IP", message = "Patient type must be OP or IP")
    private String patientType = "OP";
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
