package com.healthbox.hms_backend.modules.superadmin;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.auth.UserRepository;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.modules.payments.PaymentOrderRepository;
import com.healthbox.hms_backend.modules.tenant.Hospital;
import com.healthbox.hms_backend.modules.tenant.HospitalRepository;
import com.healthbox.hms_backend.modules.approvals.ApprovalRequest;
import com.healthbox.hms_backend.modules.approvals.ApprovalRequestRepository;
import com.healthbox.hms_backend.modules.tenant.DoctorClinicMembership;
import com.healthbox.hms_backend.modules.tenant.DoctorClinicMembershipRepository;
import com.healthbox.hms_backend.modules.fees.ServiceFeeBracket;
import com.healthbox.hms_backend.modules.fees.ServiceFeeBracketService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// Platform-owner-only view: how many hospitals/doctors/patients are on the platform,
// and how much platform fee revenue has been collected.
@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final HospitalRepository hospitalRepo;
    private final UserRepository userRepo;
    private final PatientRepository patientRepo;
    private final PaymentOrderRepository paymentOrderRepo;
    private final ApprovalRequestRepository approvalRepo;
    private final DoctorClinicMembershipRepository membershipRepo;
    private final ServiceFeeBracketService serviceFeeService;

    public SuperAdminController(HospitalRepository hospitalRepo, UserRepository userRepo,
                                 PatientRepository patientRepo, PaymentOrderRepository paymentOrderRepo,
                                 ApprovalRequestRepository approvalRepo, DoctorClinicMembershipRepository membershipRepo,
                                 ServiceFeeBracketService serviceFeeService) {
        this.hospitalRepo = hospitalRepo;
        this.userRepo = userRepo;
        this.patientRepo = patientRepo;
        this.paymentOrderRepo = paymentOrderRepo;
        this.approvalRepo = approvalRepo;
        this.membershipRepo = membershipRepo;
        this.serviceFeeService = serviceFeeService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<Hospital> hospitals = hospitalRepo.findAll();
        long totalDoctors = userRepo.countByRole(Role.ADMIN);
        long totalPatientAccounts = userRepo.countByRole(Role.PATIENT);
        long totalPatientProfiles = patientRepo.count();

        List<String> paidStatuses = List.of("PAID", "SUCCESS");
        long totalTransactions = paymentOrderRepo.countByStatuses(paidStatuses);
        double totalPlatformRevenue = paymentOrderRepo.sumPlatformFeeByStatuses(paidStatuses) / 100.0;
        double totalGrossVolume = paymentOrderRepo.sumAmountByStatuses(paidStatuses) / 100.0;

        LocalDate today = LocalDate.now();
        long transactionsToday = paymentOrderRepo.countPaidBetween(paidStatuses, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalHospitals", hospitals.size());
        summary.put("totalDoctors", totalDoctors);
        summary.put("totalPatientAccounts", totalPatientAccounts);
        summary.put("totalPatientProfiles", totalPatientProfiles);
        summary.put("totalTransactions", totalTransactions);
        summary.put("transactionsToday", transactionsToday);
        summary.put("totalPlatformRevenue", totalPlatformRevenue);
        summary.put("totalGrossVolume", totalGrossVolume);
        summary.put("hospitals", hospitalBreakdown(hospitals, paidStatuses));
        return summary;
    }

    @GetMapping("/companies")
    public List<Map<String, Object>> companies() {
        return hospitalRepo.findAllByOrderByCreatedAtDesc().stream().map(hospital -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", hospital.getId());
            row.put("name", hospital.getName());
            row.put("code", hospital.getCode());
            row.put("location", hospital.getLocation());
            row.put("approvalStatus", hospital.getApprovalStatus());
            row.put("createdAt", hospital.getCreatedAt());
            row.put("patients", patientRepo.countByHospitalId(hospital.getId()));
            row.put("doctors", userRepo.findByHospitalIdAndRole(hospital.getId(), Role.ADMIN).size());
            return row;
        }).toList();
    }

    @GetMapping("/approvals")
    public List<ApprovalRequest> approvals(@RequestParam(defaultValue = "PENDING") String status) {
        return approvalRepo.findByStatusOrderByCreatedAtDesc(status.toUpperCase(Locale.ROOT));
    }

    @GetMapping("/service-fees")
    public List<ServiceFeeBracket> serviceFees(@RequestParam(required = false) Long hospitalId) {
        return serviceFeeService.all(hospitalId);
    }

    @PostMapping("/service-fees")
    public ServiceFeeBracket createServiceFee(@RequestBody ServiceFeeBracket bracket) {
        return serviceFeeService.create(bracket);
    }

    @PatchMapping("/service-fees/{id}")
    public ServiceFeeBracket updateServiceFee(@PathVariable Long id, @RequestBody ServiceFeeBracket bracket) {
        return serviceFeeService.update(id, bracket);
    }

    @DeleteMapping("/service-fees/{id}")
    public ResponseEntity<Void> deactivateServiceFee(@PathVariable Long id) {
        serviceFeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/approvals/{id}")
    public ApprovalRequest review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ApprovalRequest request = approvalRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Approval request not found"));
        String decision = body.getOrDefault("status", "").toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "REJECTED", "CANCELLED").contains(decision)) {
            throw new IllegalArgumentException("Invalid approval status");
        }
        request.setStatus(decision);
        request.setReviewedBy("SUPER_ADMIN");
        request.setReviewedAt(LocalDateTime.now());
        if (request.getHospitalId() != null) {
            Hospital hospital = hospitalRepo.findById(request.getHospitalId()).orElseThrow(() -> new IllegalArgumentException("Company not found"));
            hospital.setApprovalStatus(decision);
            hospital.setApprovalReason(body.get("reason"));
            hospital.setApprovedAt("APPROVED".equals(decision) ? LocalDateTime.now() : null);
            hospitalRepo.save(hospital);
        }
        return approvalRepo.save(request);
    }

    @PostMapping("/doctors/{doctorPhno}/clinics/{hospitalId}")
    public DoctorClinicMembership assignDoctorToClinic(@PathVariable String doctorPhno, @PathVariable Long hospitalId,
                                                       @RequestBody(required = false) Map<String, String> body) {
        UserRepository userRepository = this.userRepo;
        var doctor = userRepository.findById(doctorPhno).filter(user -> user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        String accountId = body == null ? null : body.get("razorpayAccountId");
        if (accountId != null && !accountId.isBlank()) {
            doctor.setRazorpayAccountId(accountId.trim());
            userRepository.save(doctor);
        }
        hospitalRepo.findById(hospitalId).orElseThrow(() -> new IllegalArgumentException("Company not found"));
        if (membershipRepo.findByDoctorPhnoAndHospitalIdAndActiveTrue(doctorPhno, hospitalId).isPresent()) {
            return membershipRepo.findByDoctorPhnoAndHospitalIdAndActiveTrue(doctorPhno, hospitalId).get();
        }
        DoctorClinicMembership membership = new DoctorClinicMembership();
        membership.setDoctorPhno(doctorPhno);
        membership.setHospitalId(hospitalId);
        return membershipRepo.save(membership);
    }

    private List<Map<String, Object>> hospitalBreakdown(List<Hospital> hospitals, List<String> statuses) {
        Map<Long, Object[]> byHospital = paymentOrderRepo.summarizeByHospital(statuses).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> row));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Hospital h : hospitals) {
            Object[] orderSummary = byHospital.get(h.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hospitalId", h.getId());
            row.put("name", h.getName());
            row.put("code", h.getCode());
            row.put("patients", patientRepo.findByHospitalId(h.getId()).size());
            row.put("transactions", orderSummary == null ? 0 : ((Number) orderSummary[1]).longValue());
            row.put("platformRevenue", orderSummary == null ? 0 : ((Number) orderSummary[2]).longValue() / 100.0);
            result.add(row);
        }
        return result;
    }
}
