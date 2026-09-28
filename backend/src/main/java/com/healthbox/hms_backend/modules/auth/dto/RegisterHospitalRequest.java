package com.healthbox.hms_backend.modules.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterHospitalRequest {
    private String hospitalName;
    private String hospitalCode;
    private String location;
    private String adminPhno;
    private String username;
    private String password;
    private String email;
    private String otp;
}
