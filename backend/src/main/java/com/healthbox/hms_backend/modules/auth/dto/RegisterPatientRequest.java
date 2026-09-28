package com.healthbox.hms_backend.modules.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterPatientRequest {
    private String phno;
    private String phoneNumber;
    private String username;
    private String password;
    private String email;
    private String otp;
    private String hospitalCode; // the doctor/practice this patient is registering under
}
