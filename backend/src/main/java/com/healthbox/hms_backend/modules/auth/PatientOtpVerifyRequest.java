package com.healthbox.hms_backend.modules.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientOtpVerifyRequest {
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String otp;
    @NotBlank
    private String hospitalCode;
    private String name;
}
