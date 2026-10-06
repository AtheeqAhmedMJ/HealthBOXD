package com.healthbox.hms_backend.modules.auth;

import com.healthbox.hms_backend.modules.auth.dto.*;
import com.healthbox.hms_backend.modules.otp.OtpService;
import com.healthbox.hms_backend.modules.tenant.Hospital;
import com.healthbox.hms_backend.modules.tenant.HospitalRepository;
import com.healthbox.hms_backend.modules.tenant.DoctorClinicMembership;
import com.healthbox.hms_backend.modules.tenant.DoctorClinicMembershipRepository;
import com.healthbox.hms_backend.modules.appointments.AppointmentService;
import com.healthbox.hms_backend.modules.approvals.ApprovalRequest;
import com.healthbox.hms_backend.modules.approvals.ApprovalRequestRepository;
import com.healthbox.hms_backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.security.SecureRandom;
import com.healthbox.hms_backend.modules.patients.Patient;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final HospitalRepository hospitalRepository;
    private final OtpService otpService;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final DoctorClinicMembershipRepository membershipRepository;
    private final AppointmentService appointmentService;

    private final SecureRandom secureRandom = new SecureRandom();

    // Single login for everyone — the returned role tells the frontend which dashboard to render.
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (user.getRole() == Role.ADMIN && user.getHospitalId() != null) {
            Hospital hospital = hospitalRepository.findById(user.getHospitalId()).orElse(null);
            if (hospital != null && !"APPROVED".equals(hospital.getApprovalStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clinic approval is pending");
            }
        }
        String token = jwtTokenProvider.generateToken(user);
        return new LoginResponse(token, user.getUsername(), user.getRole().name(), user.getHospitalId(), user.getPhno());
    }

    // Step 1 of any self-registration: email OTP. purpose = REGISTER_ADMIN | REGISTER_PATIENT
    @PostMapping("/otp/request")
    public void requestOtp(@RequestBody OtpRequest req) {
        otpService.requestPhoneOtp(req.getPhoneNumber(), req.getEmail(), req.getPurpose());
    }

    // Doctor self-registers: creates their practice (tenant) + their own ADMIN account.
    @PostMapping("/register-hospital")
    public LoginResponse registerHospital(@RequestBody RegisterHospitalRequest req) {
        otpService.verifyOtp(req.getEmail(), "REGISTER_ADMIN", req.getOtp());

        if (hospitalRepository.existsByCode(req.getHospitalCode())) {
            throw new IllegalArgumentException("Hospital code already in use");
        }
        Hospital hospital = new Hospital();
        hospital.setName(req.getHospitalName());
        hospital.setCode(req.getHospitalCode());
        hospital.setLocation(req.getLocation());
        hospital.setApprovalStatus("PENDING");
        hospital = hospitalRepository.save(hospital);

        ApprovalRequest approval = new ApprovalRequest();
        approval.setRequestType("CLINIC_REGISTRATION");
        approval.setHospitalId(hospital.getId());
        approval.setRequestedBy(req.getAdminPhno());
        approval.setDetails(hospital.getName() + " · " + hospital.getLocation());
        approvalRequestRepository.save(approval);

        User admin = userService.saveUser(req.getAdminPhno(), req.getUsername(), req.getPassword(),
                req.getEmail(), Role.ADMIN, hospital.getId());
        DoctorClinicMembership membership = new DoctorClinicMembership();
        membership.setDoctorPhno(admin.getPhno());
        membership.setHospitalId(hospital.getId());
        membershipRepository.save(membership);
        String token = jwtTokenProvider.generateToken(admin);
        return new LoginResponse(token, admin.getUsername(), admin.getRole().name(), admin.getHospitalId(), admin.getPhno());
    }

    @GetMapping("/clinics")
    public List<Map<String, Object>> clinics() {
        AppUserPrincipal actor = currentUser.get();
        if (actor.getRole() != Role.ADMIN) throw new AccessDeniedException("Doctors only");
        return membershipRepository.findByDoctorPhnoAndActiveTrue(actor.getPhno()).stream().map(membership -> {
            Hospital hospital = hospitalRepository.findById(membership.getHospitalId()).orElseThrow();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", hospital.getId());
            row.put("name", hospital.getName());
            row.put("code", hospital.getCode());
            row.put("location", hospital.getLocation() == null ? "" : hospital.getLocation());
            row.put("approvalStatus", hospital.getApprovalStatus());
            row.put("active", hospital.getId().equals(actor.getHospitalId()));
            return row;
        }).toList();
    }

    @PostMapping("/clinics/{hospitalId}/switch")
    public LoginResponse switchClinic(@PathVariable Long hospitalId) {
        AppUserPrincipal actor = currentUser.get();
        if (actor.getRole() != Role.ADMIN) throw new AccessDeniedException("Doctors only");
        membershipRepository.findByDoctorPhnoAndHospitalIdAndActiveTrue(actor.getPhno(), hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Doctor is not assigned to this clinic"));
        Hospital hospital = hospitalRepository.findById(hospitalId).orElseThrow(() -> new IllegalArgumentException("Clinic not found"));
        if (!"APPROVED".equals(hospital.getApprovalStatus())) throw new AccessDeniedException("Clinic is not approved");
        User user = userRepository.findById(actor.getPhno()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!hospitalId.equals(actor.getHospitalId())) {
            appointmentService.pauseFutureDoctorAppointments(actor.getHospitalId(), actor.getPhno());
        }
        return new LoginResponse(jwtTokenProvider.generateToken(user, hospitalId), user.getUsername(), user.getRole().name(), hospitalId, user.getPhno());
    }

    // Patient self-registers for free under a doctor's practice (identified by hospitalCode).
    // This only creates a login identity — the clinical Patient profile is created later,
    // gated behind the first paid consultation.
    @PostMapping("/register-patient")
    public LoginResponse registerPatient(@RequestBody RegisterPatientRequest req) {
        String phone = req.getPhoneNumber() == null || req.getPhoneNumber().isBlank() ? req.getPhno() : req.getPhoneNumber();
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
            otpService.verifyOtp(req.getEmail(), "REGISTER_PATIENT", req.getOtp());
        } else {
            otpService.verifyPhoneOtp(phone, "REGISTER_PATIENT", req.getOtp());
        }

        Hospital hospital = hospitalRepository.findByCode(req.getHospitalCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown hospital code"));

        String username = req.getUsername() == null || req.getUsername().isBlank() ? phone : req.getUsername();
        String email = req.getEmail() == null || req.getEmail().isBlank() ? phone + "@phone.invalid" : req.getEmail();
        User patient = userService.saveUser(phone, username, req.getPassword(), email, Role.PATIENT, hospital.getId());
        String token = jwtTokenProvider.generateToken(patient);
        return new LoginResponse(token, patient.getUsername(), patient.getRole().name(), patient.getHospitalId(), patient.getPhno());
    }

    @PostMapping("/patient/otp/verify")
    public LoginResponse verifyPatientOtp(@RequestBody PatientOtpVerifyRequest request) {
        otpService.verifyPhoneOtp(request.getPhoneNumber(), "PATIENT_LOGIN", request.getOtp());
        Hospital hospital = hospitalRepository.findByCode(request.getHospitalCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown hospital code"));
        User patientUser = userService.findByPhone(request.getPhoneNumber());
        if (patientUser == null) {
            String password = Long.toUnsignedString(secureRandom.nextLong());
            patientUser = userService.saveUser(request.getPhoneNumber(), request.getPhoneNumber(), password,
                    request.getPhoneNumber() + "@phone.invalid", Role.PATIENT, hospital.getId());
        }
        Patient patient = patientRepository.findByPhno(request.getPhoneNumber()).orElseGet(Patient::new);
        patient.setPhno(request.getPhoneNumber());
        patient.setName(request.getName() == null || request.getName().isBlank() ? request.getPhoneNumber() : request.getName().trim());
        patient.setHospitalId(patientUser.getHospitalId());
        patientRepository.save(patient);
        String token = jwtTokenProvider.generateToken(patientUser);
        return new LoginResponse(token, patientUser.getUsername(), patientUser.getRole().name(), patientUser.getHospitalId(), patientUser.getPhno());
    }

    @GetMapping("/profile")
    public Map<String, Object> profile() {
        AppUserPrincipal actor = currentUser.get();
        User user = userRepository.findById(actor.getPhno()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Hospital hospital = user.getHospitalId() == null ? null : hospitalRepository.findById(user.getHospitalId()).orElse(null);
        return Map.of("username", user.getUsername(), "email", user.getEmail(), "phno", user.getPhno(),
            "hospitalName", hospital == null ? "" : hospital.getName(),
            "hospitalCode", hospital == null ? "" : hospital.getCode(),
            "location", hospital == null || hospital.getLocation() == null ? "" : hospital.getLocation());
    }

    @PatchMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> request) {
        AppUserPrincipal actor = currentUser.get();
        User user = userRepository.findById(actor.getPhno()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String username = request.get("username");
        if (username != null && !username.isBlank() && !username.equals(user.getUsername()) && userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (username != null && !username.isBlank()) user.setUsername(username.trim());
        if (request.get("email") != null && !request.get("email").isBlank()) user.setEmail(request.get("email").trim().toLowerCase());
        userRepository.save(user);
        return profile();
    }

    @PostMapping("/change-password")
    public void changePassword(@RequestBody Map<String, String> request) {
        AppUserPrincipal actor = currentUser.get();
        User user = userRepository.findById(actor.getPhno()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(request.getOrDefault("currentPassword", ""), user.getPassword())) {
            throw new AccessDeniedException("Current password is incorrect");
        }
        String next = request.get("newPassword");
        if (next == null || next.length() < 8) throw new IllegalArgumentException("New password must be at least 8 characters");
        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);
    }

    @PostMapping("/logout")
    public void logout() {
        // JWT access tokens are short-lived and stateless; the client removes its token.
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
