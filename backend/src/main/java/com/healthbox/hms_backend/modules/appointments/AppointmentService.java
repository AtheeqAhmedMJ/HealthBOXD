package com.healthbox.hms_backend.modules.appointments;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.patients.Patient;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import com.healthbox.hms_backend.shared.events.AppointmentCreated;
import com.healthbox.hms_backend.shared.events.DomainEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

// Booking a slot is free and doesn't require an existing Patient profile —
// the profile only gets created once the consultation itself is paid for.
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final PatientRepository patientRepo;
    private final CurrentUser currentUser;
    private final DomainEventPublisher events;

    public AppointmentService(AppointmentRepository appointmentRepo, PatientRepository patientRepo, CurrentUser currentUser) {
        this(appointmentRepo, patientRepo, currentUser, DomainEventPublisher.noop());
    }

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepo, PatientRepository patientRepo,
                               CurrentUser currentUser, DomainEventPublisher events) {
        this.appointmentRepo = appointmentRepo;
        this.patientRepo = patientRepo;
        this.currentUser = currentUser;
        this.events = events;
    }

    @Transactional
    public Appointment create(Appointment a) {
        AppUserPrincipal me = currentUser.get();

        if (me.getRole() == Role.SUPER_ADMIN) throw new AccessDeniedException("Super admin does not manage appointments");
        if (a.getPatientPhno() == null || a.getPatientPhno().isBlank()) throw new IllegalArgumentException("Patient phone number is required");
        if (a.getDate() == null) throw new IllegalArgumentException("Appointment date is required");
        if (a.getAppointmentTime() == null) throw new IllegalArgumentException("Appointment time is required");
        if (a.getIdempotencyKey() != null) {
            Optional<Appointment> existing = appointmentRepo.findByIdempotencyKey(a.getIdempotencyKey());
            if (existing.isPresent()) return existing.get();
        }
        if (appointmentRepo.existsByHospitalIdAndPatientPhnoAndDateAndStatusNotIn(
                me.getHospitalId(), a.getPatientPhno(), a.getDate(), List.of("CANCELLED", "NO_SHOW", "PAUSED"))) {
            throw new IllegalArgumentException("Appointment already exists for this patient on " + a.getDate());
        }
        if (a.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Appointment date cannot be in the past.");
        }
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(a.getPatientPhno())) {
            throw new AccessDeniedException("You can only book for yourself");
        }

        a.setHospitalId(me.getHospitalId());
        a.setStatus("BOOKED");
        if (a.getAppointmentType() == null || a.getAppointmentType().isBlank()) a.setAppointmentType("PRE_BOOKED");
        a.setUpdatedAt(java.time.LocalDateTime.now());
        a.setDoctorPhno(me.getRole() == Role.ADMIN ? me.getPhno() : a.getDoctorPhno());
        Appointment saved = appointmentRepo.save(a);
        events.publish(new AppointmentCreated(saved.getId(), saved.getHospitalId(), saved.getPatientPhno()));
        return saved;
    }

    @Transactional
    public Appointment changeStatus(Long id, String requestedStatus) {
        Appointment appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        assertAccessible(appointment);
        String next = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("BOOKED", "CHECKED_IN", "IN_CONSULTATION", "COMPLETED", "CANCELLED", "NO_SHOW").contains(next)) {
            throw new IllegalArgumentException("Invalid appointment status");
        }
        if (!allowedTransitions(appointment.getStatus()).contains(next)) {
            throw new IllegalStateException("Appointment cannot transition from " + appointment.getStatus() + " to " + next);
        }
        appointment.setStatus(next);
        appointment.setUpdatedAt(java.time.LocalDateTime.now());
        return appointmentRepo.save(appointment);
    }

    private Set<String> allowedTransitions(String current) {
        return switch (current) {
            case "BOOKED" -> Set.of("CHECKED_IN", "CANCELLED", "NO_SHOW");
            case "CHECKED_IN" -> Set.of("IN_CONSULTATION", "CANCELLED", "NO_SHOW");
            case "IN_CONSULTATION" -> Set.of("COMPLETED");
            case "COMPLETED", "CANCELLED", "NO_SHOW" -> Set.of();
            default -> Set.of();
        };
    }

    public List<Appointment> getAll() {
        AppUserPrincipal me = currentUser.get();
        return switch (me.getRole()) {
            case ADMIN -> appointmentRepo.findByHospitalId(me.getHospitalId());
            case PATIENT -> appointmentRepo.findByPatientPhno(me.getPhno());
            case SUPER_ADMIN -> throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        };
    }

    public List<Appointment> getByPatientPhno(String phno) {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() == Role.SUPER_ADMIN) throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(phno)) throw new AccessDeniedException("Not your own record");
        List<Appointment> results = appointmentRepo.findByPatientPhno(phno);
        if (me.getRole() == Role.ADMIN) {
            results = results.stream().filter(a -> a.getHospitalId().equals(me.getHospitalId())).toList();
        }
        return results;
    }

    public void delete(Long id) {
        Appointment a = appointmentRepo.findById(id).orElseThrow(() -> new RuntimeException("Appointment not found"));
        assertAccessible(a);
        appointmentRepo.deleteById(id);
    }

    @Transactional
    public int pauseFutureDoctorAppointments(Long hospitalId, String doctorPhno) {
        int changed = 0;
        for (Appointment appointment : appointmentRepo.findByHospitalIdAndDoctorPhno(hospitalId, doctorPhno)) {
            if (appointment.getDate() != null && !appointment.getDate().isBefore(LocalDate.now())
                    && Set.of("BOOKED", "CHECKED_IN").contains(appointment.getStatus())) {
                appointment.setStatus("PAUSED");
                appointment.setUpdatedAt(java.time.LocalDateTime.now());
                appointmentRepo.save(appointment);
                changed++;
            }
        }
        return changed;
    }

    // Best-effort display join: shows real Patient details if a profile already exists
    // (i.e. they've had at least one paid consultation), otherwise falls back to the
    // name captured at booking time.
    public List<Map<String, Object>> getAllWithPatientDetails() {
        List<Map<String, Object>> response = new ArrayList<>();
        for (Appointment a : getAll()) {
            Optional<Patient> patientOpt = patientRepo.findByPhno(a.getPatientPhno());
            Map<String, Object> entry = new HashMap<>();
            entry.put("appointmentId", a.getId());
            entry.put("date", a.getDate());
            entry.put("status", a.getStatus());
            entry.put("createdAt", a.getCreatedAt());
            entry.put("phno", a.getPatientPhno());
            entry.put("name", patientOpt.map(Patient::getName).orElse(a.getPatientName()));
            entry.put("age", patientOpt.map(Patient::getAge).orElse(null));
            entry.put("gender", patientOpt.map(Patient::getGender).orElse(null));
            entry.put("hasProfile", patientOpt.isPresent());
            response.add(entry);
        }
        return response;
    }

    private void assertAccessible(Appointment a) {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() == Role.SUPER_ADMIN) throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        if (!a.getHospitalId().equals(me.getHospitalId())) throw new AccessDeniedException("Cross-tenant access denied");
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(a.getPatientPhno())) throw new AccessDeniedException("Not your own record");
    }
}
