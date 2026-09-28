package com.healthbox.hms_backend.modules.consultations;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.appointments.Appointment;
import com.healthbox.hms_backend.modules.appointments.AppointmentRepository;
import com.healthbox.hms_backend.modules.patients.Patient;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.modules.prescriptions.Prescription;
import com.healthbox.hms_backend.modules.prescriptions.PrescriptionRepository;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.healthbox.hms_backend.modules.consultations.dto.ConsultationRequest;
import com.healthbox.hms_backend.modules.consultations.dto.ConsultationResponse;
import com.healthbox.hms_backend.modules.consultations.dto.ConsultationUpdateRequest;

@Service
public class ConsultationService {
    private final ConsultationRepository repository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final CurrentUser currentUser;

    public ConsultationService(ConsultationRepository repository, PatientRepository patientRepository,
                               AppointmentRepository appointmentRepository, PrescriptionRepository prescriptionRepository,
                               CurrentUser currentUser) {
        this.repository = repository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public ConsultationResponse create(ConsultationRequest request) {
        AppUserPrincipal actor = requireDoctor();
        validateAppointment(actor, request.getAppointmentId(), request.getPatientPhno());
        Patient patient = patientRepository.findByPhno(request.getPatientPhno()).orElseGet(() -> {
            Patient pending = new Patient();
            pending.setPhno(request.getPatientPhno());
            pending.setName(findAppointmentName(request.getAppointmentId(), request.getPatientPhno()));
            pending.setHospitalId(actor.getHospitalId());
            pending.setAssignedDoctorPhno(actor.getPhno());
            return patientRepository.save(pending);
        });
        if (request.getAppointmentId() != null) {
            var existing = repository.findByAppointmentId(request.getAppointmentId());
            if (existing.isPresent()) return response(accessible(existing.get().getId()));
        }
        Consultation consultation = new Consultation();
        consultation.setPatientId(patient.getId());
        consultation.setPatientPhno(patient.getPhno());
        consultation.setDoctorPhno(actor.getPhno());
        consultation.setAppointmentId(request.getAppointmentId());
        consultation.setPatientType(request.getPatientType() == null ? "OP" : request.getPatientType());
        consultation.setInpatientDetails(request.getInpatientDetails());
        consultation.setInjections(request.getInjections());
        consultation.setMedicines(request.getMedicines());
        apply(consultation, request);
        return response(repository.save(consultation));
    }

    @Transactional
    public ConsultationResponse update(Long id, ConsultationUpdateRequest request) {
        Consultation consultation = accessible(id);
        if (!List.of("CREATED", "IN_PROGRESS").contains(consultation.getStatus())) {
            throw new IllegalStateException("Only an active consultation draft can be edited");
        }
        apply(consultation, request);
        consultation.setUpdatedAt(LocalDateTime.now());
        return response(repository.save(consultation));
    }

    @Transactional
    public ConsultationResponse transition(Long id, String next) {
        Consultation consultation = accessible(id);
        String current = consultation.getStatus();
        if (!allowed(current, next)) {
            throw new IllegalStateException("Consultation cannot transition from " + current + " to " + next);
        }
        consultation.setStatus(next);
        consultation.setUpdatedAt(LocalDateTime.now());
        if ("FINALIZED".equals(next)) consultation.setFinalizedAt(LocalDateTime.now());
        Consultation saved = repository.save(consultation);
        if ("AWAITING_PAYMENT".equals(next)) ensureDraftPrescription(saved);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse get(Long id) {
        return response(accessible(id));
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> patientRecords() {
        AppUserPrincipal actor = currentUser.get();
        if (actor.getRole() != Role.PATIENT) throw new AccessDeniedException("Patient access required");
        return repository.findByPatientPhnoAndStatusOrderByCreatedAtDesc(actor.getPhno(), "FINALIZED")
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> recordsForPatient(String phno) {
        AppUserPrincipal actor = requireDoctor();
        return repository.findByPatientPhnoAndStatusOrderByCreatedAtDesc(phno, "FINALIZED").stream()
                .filter(consultation -> actor.getPhno().equals(consultation.getDoctorPhno()))
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> recordsForDoctor() {
        AppUserPrincipal actor = requireDoctor();
        return repository.findByDoctorPhnoAndStatusOrderByCreatedAtDesc(actor.getPhno(), "FINALIZED")
                .stream().map(this::response).toList();
    }

    private void ensureDraftPrescription(Consultation consultation) {
        if (prescriptionRepository.findByConsultationId(consultation.getId()).isPresent()) return;
        Prescription prescription = new Prescription();
        prescription.setPatientPhno(consultation.getPatientPhno());
        prescription.setAppointmentId(consultation.getAppointmentId());
        prescription.setConsultationId(consultation.getId());
        prescription.setHospitalId(currentUser.get().getHospitalId());
        prescription.setDoctorPhno(consultation.getDoctorPhno());
        prescription.setSymptoms(consultation.getSymptoms());
        prescription.setDiagnosis(consultation.getDiagnosis());
        prescription.setNotes(consultation.getGeneralNotes());
        prescription.setBp(consultation.getBp());
        prescription.setGrbs(consultation.getGrbs());
        prescription.setSpo2(consultation.getSpo2());
        prescription.setTemp(consultation.getTemperature());
        prescription.setNextVisitDate(consultation.getNextVisitDate());
        prescription.setStatus("DRAFT");
        prescriptionRepository.save(prescription);
    }

    private void validateAppointment(AppUserPrincipal actor, Long appointmentId, String patientPhno) {
        if (appointmentId == null) return;
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!actor.getHospitalId().equals(appointment.getHospitalId()) || !patientPhno.equals(appointment.getPatientPhno())) {
            throw new AccessDeniedException("Appointment does not belong to this clinic and patient");
        }
    }

    private String findAppointmentName(Long appointmentId, String fallback) {
        if (appointmentId == null) return fallback;
        return appointmentRepository.findById(appointmentId)
                .map(Appointment::getPatientName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(fallback);
    }

    private Consultation accessible(Long id) {
        Consultation consultation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found"));
        AppUserPrincipal actor = currentUser.get();
        if (actor.getRole() == Role.PATIENT) {
            if (!actor.getPhno().equals(consultation.getPatientPhno()) || !"FINALIZED".equals(consultation.getStatus())) {
                throw new AccessDeniedException("This medical record is not available");
            }
        } else if (actor.getRole() == Role.ADMIN && !actor.getPhno().equals(consultation.getDoctorPhno())) {
            throw new AccessDeniedException("Consultation belongs to another doctor");
        } else if (actor.getRole() == Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Super admin does not access clinical records");
        }
        return consultation;
    }

    private AppUserPrincipal requireDoctor() {
        AppUserPrincipal actor = currentUser.get();
        if (actor.getRole() != Role.ADMIN) throw new AccessDeniedException("Doctor access required");
        return actor;
    }

    private boolean allowed(String current, String next) {
        return switch (current) {
            case "CREATED" -> "IN_PROGRESS".equals(next);
            case "IN_PROGRESS" -> "AWAITING_PAYMENT".equals(next);
            case "AWAITING_PAYMENT" -> false;
            case "FINALIZED" -> false;
            default -> false;
        };
    }

    private void apply(Consultation consultation, ConsultationRequest request) {
        consultation.setSymptoms(request.getSymptoms());
        consultation.setDiagnosis(request.getDiagnosis());
        consultation.setClinicalNotes(request.getClinicalNotes());
        consultation.setGeneralNotes(request.getGeneralNotes());
        consultation.setBp(request.getBp());
        consultation.setGrbs(request.getGrbs());
        consultation.setSpo2(request.getSpo2());
        consultation.setTemperature(request.getTemperature());
        consultation.setNextVisitDate(request.getNextVisitDate());
    }

    private void apply(Consultation consultation, ConsultationUpdateRequest request) {
        if (request.getPatientType() != null) consultation.setPatientType(request.getPatientType());
        consultation.setInpatientDetails(request.getInpatientDetails());
        consultation.setInjections(request.getInjections());
        consultation.setMedicines(request.getMedicines());
        consultation.setSymptoms(request.getSymptoms());
        consultation.setDiagnosis(request.getDiagnosis());
        consultation.setClinicalNotes(request.getClinicalNotes());
        consultation.setGeneralNotes(request.getGeneralNotes());
        consultation.setBp(request.getBp());
        consultation.setGrbs(request.getGrbs());
        consultation.setSpo2(request.getSpo2());
        consultation.setTemperature(request.getTemperature());
        consultation.setNextVisitDate(request.getNextVisitDate());
    }

    private ConsultationResponse response(Consultation c) {
        return new ConsultationResponse(c.getId(), c.getPatientPhno(), c.getDoctorPhno(), c.getAppointmentId(), c.getStatus(), c.getPatientType(), c.getInpatientDetails(), c.getInjections(), c.getMedicines(),
                c.getSymptoms(), c.getDiagnosis(), c.getClinicalNotes(), c.getGeneralNotes(), c.getBp(), c.getGrbs(),
                c.getSpo2(), c.getTemperature(), c.getNextVisitDate(), c.getCreatedAt(), c.getUpdatedAt(), c.getFinalizedAt());
    }
}
