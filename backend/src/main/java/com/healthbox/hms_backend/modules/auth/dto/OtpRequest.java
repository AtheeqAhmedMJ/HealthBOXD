package com.healthbox.hms_backend.modules.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequest {
    private String email;
    private String phoneNumber;
    private String purpose; // REGISTER_ADMIN or REGISTER_PATIENT
}
