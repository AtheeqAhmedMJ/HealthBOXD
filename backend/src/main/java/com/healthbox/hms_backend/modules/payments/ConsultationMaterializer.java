package com.healthbox.hms_backend.modules.payments;

import com.healthbox.hms_backend.modules.billing.Billing;
import com.healthbox.hms_backend.modules.billing.BillingRepository;
import com.healthbox.hms_backend.modules.patients.Patient;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.modules.prescriptions.Prescription;
import com.healthbox.hms_backend.modules.prescriptions.PrescriptionRepository;
import com.healthbox.hms_backend.modules.prescriptions.PrescriptionMedication;
import com.healthbox.hms_backend.modules.prescriptions.PrescriptionMedicationRepository;
import com.healthbox.hms_backend.modules.consultations.Consultation;
import com.healthbox.hms_backend.modules.consultations.ConsultationRepository;
import com.healthbox.hms_backend.modules.notifications.NotificationService;
import com.healthbox.hms_backend.shared.events.DomainEventPublisher;
import com.healthbox.hms_backend.shared.events.PaymentCompleted;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Separate bean (not a method on PaymentService) so @Transactional is honoured —
 * calling it from within the same class would bypass the Spring proxy and silently
 * skip the transaction, breaking the all-or-nothing guarantee this depends on.
 */
@Service
public class ConsultationMaterializer {

    private final PaymentOrderRepository orderRepo;
    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final BillingRepository billingRepo;
    private final PrescriptionMedicationRepository medicationRepo;
    private final ConsultationRepository consultationRepo;
    private final NotificationService notificationService;
    private final DomainEventPublisher events;
    private final DoctorPayoutService doctorPayoutService;

    public ConsultationMaterializer(PaymentOrderRepository orderRepo, PatientRepository patientRepo,
                                     PrescriptionRepository prescriptionRepo, BillingRepository billingRepo,
                                     PrescriptionMedicationRepository medicationRepo,
                                     ConsultationRepository consultationRepo,
                                     NotificationService notificationService) {
        this(orderRepo, patientRepo, prescriptionRepo, billingRepo, medicationRepo, consultationRepo,
                            notificationService, DomainEventPublisher.noop(), null);
    }

    public ConsultationMaterializer(PaymentOrderRepository orderRepo, PatientRepository patientRepo,
                                     PrescriptionRepository prescriptionRepo, BillingRepository billingRepo,
                                     PrescriptionMedicationRepository medicationRepo,
                                     ConsultationRepository consultationRepo,
                                     NotificationService notificationService, DomainEventPublisher events) {
                        this(orderRepo, patientRepo, prescriptionRepo, billingRepo, medicationRepo, consultationRepo,
                            notificationService, events, null);
                        }

                        @org.springframework.beans.factory.annotation.Autowired
                        public ConsultationMaterializer(PaymentOrderRepository orderRepo, PatientRepository patientRepo,
                                         PrescriptionRepository prescriptionRepo, BillingRepository billingRepo,
                                         PrescriptionMedicationRepository medicationRepo,
                                         ConsultationRepository consultationRepo,
                                         NotificationService notificationService, DomainEventPublisher events,
                                         DoctorPayoutService doctorPayoutService) {
        this.orderRepo = orderRepo;
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.billingRepo = billingRepo;
        this.medicationRepo = medicationRepo;
        this.consultationRepo = consultationRepo;
        this.notificationService = notificationService;
        this.events = events;
        this.doctorPayoutService = doctorPayoutService;
    }

    @Transactional
    public void materialize(PaymentOrder po, String razorpayPaymentId) {
        po = orderRepo.findByIdForUpdate(po.getId())
            .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        if ("SUCCESS".equals(po.getStatus()) || "PAID".equals(po.getStatus())) return;

        Map<String, Object> payload = po.getPayload() == null ? Map.of() : po.getPayload();
        Consultation consultation = po.getConsultationId() == null ? null : consultationRepo.findById(po.getConsultationId())
            .orElseThrow(() -> new IllegalArgumentException("Consultation not found"));

        Patient patient = patientRepo.findByPhno(po.getPatientPhno()).orElseGet(Patient::new);
        patient.setPhno(po.getPatientPhno());
        patient.setName((String) payload.getOrDefault("patientName", po.getPatientPhno()));
        if (payload.get("patientAge") != null) patient.setAge(((Number) payload.get("patientAge")).intValue());
        if (payload.get("patientGender") != null) patient.setGender((String) payload.get("patientGender"));
        patient.setHospitalId(po.getHospitalId());
        if (patient.getAssignedDoctorPhno() == null) patient.setAssignedDoctorPhno(po.getDoctorPhno());
        patientRepo.save(patient);

        Prescription rx = po.getPrescriptionId() != null
            ? prescriptionRepo.findById(po.getPrescriptionId()).orElseThrow(() -> new IllegalArgumentException("Prescription draft not found"))
            : po.getConsultationId() != null
                ? prescriptionRepo.findByConsultationId(po.getConsultationId()).orElseGet(Prescription::new)
                : new Prescription();
        rx.setPatientPhno(po.getPatientPhno());
        rx.setAppointmentId(po.getAppointmentId());
        rx.setConsultationId(po.getConsultationId());
        rx.setPatientType(consultation == null ? String.valueOf(payload.getOrDefault("patientType", "OP")) : consultation.getPatientType());
        rx.setInpatientDetails(consultation == null ? null : consultation.getInpatientDetails());
        rx.setInjections(consultation == null ? null : consultation.getInjections());
        rx.setSymptoms((String) payload.get("symptoms"));
        rx.setBp((String) payload.get("bp"));
        rx.setSpo2((String) payload.get("spo2"));
        rx.setGrbs((String) payload.get("grbs"));
        rx.setTemp((String) payload.get("temp"));
        Object meds = consultation != null && consultation.getMedicines() != null
            ? consultation.getMedicines()
            : payload.get("medicines");
        if (meds instanceof List<?> medicationList) {
            List<Map<String, Object>> normalizedMedicines = new ArrayList<>();
            for (Object medicationValue : medicationList) {
                if (medicationValue instanceof Map<?, ?> medication) {
                    Map<String, Object> normalized = new HashMap<>();
                    medication.forEach((key, value) -> normalized.put(String.valueOf(key), value));
                    normalizedMedicines.add(normalized);
                }
            }
            rx.setMedicines(normalizedMedicines);
        }
        rx.setRemarks((String) payload.get("remarks"));
        rx.setHospitalId(po.getHospitalId());
        rx.setDoctorPhno(po.getDoctorPhno());
        if (consultation != null) {
            rx.setSymptoms(consultation.getSymptoms());
            rx.setDiagnosis(consultation.getDiagnosis());
            rx.setNotes(consultation.getGeneralNotes());
            rx.setBp(consultation.getBp());
            rx.setGrbs(consultation.getGrbs());
            rx.setSpo2(consultation.getSpo2());
            rx.setTemp(consultation.getTemperature());
            rx.setNextVisitDate(consultation.getNextVisitDate());
        }
        rx.setStatus("FINALIZED");
        rx.setFinalizedAt(LocalDateTime.now());
        rx = prescriptionRepo.save(rx);

        if (meds instanceof List<?> medicationList) {
            for (Object medicationValue : medicationList) {
                if (!(medicationValue instanceof Map<?, ?> medication)) continue;
                String type = requiredValue(medication, "type", "medicationType").toUpperCase();
                if (!List.of("TAB", "CAP", "SYRUP", "OINT", "POWDER", "INJC", "OTHERS").contains(type)) {
                    throw new IllegalArgumentException("Unsupported medication type");
                }
                PrescriptionMedication entry = new PrescriptionMedication();
                entry.setPrescriptionId(rx.getId());
                entry.setMedicationType(type);
                entry.setMedicationName(requiredValue(medication, "name", "medicationName"));
                entry.setDosage(requiredValue(medication, "dosage"));
                entry.setQuantity(requiredValue(medication, "quantity"));
                entry.setDuration(requiredValue(medication, "duration"));
                entry.setFoodInstruction(value(medication, "foodInstruction", "food").orElse(null));
                entry.setAdditionalInstructions(value(medication, "instructions", "additionalInstructions").orElse(null));
                medicationRepo.save(entry);
            }
        }

        Billing bill = new Billing();
        bill.setPatientPhno(po.getPatientPhno());
        bill.setAppointmentId(po.getAppointmentId());
        bill.setTotalAmount(po.getAmountPaise() / 100.0);
        bill.setPaymentStatus("PAID");
        bill.setPaymentMode("ONLINE");
        bill.setHospitalId(po.getHospitalId());
        billingRepo.save(bill);

        po.setStatus("SUCCESS");
        po.setPaidAt(LocalDateTime.now());
        po.setRazorpayPaymentId(razorpayPaymentId);
        po.setUpdatedAt(LocalDateTime.now());
        if (doctorPayoutService != null) doctorPayoutService.transfer(po);
        orderRepo.save(po);
        if (consultation != null) {
            consultation.setStatus("FINALIZED");
            consultation.setFinalizedAt(LocalDateTime.now());
            consultation.setUpdatedAt(LocalDateTime.now());
            consultationRepo.save(consultation);
        }
        notificationService.queuePrescriptionReady(po.getPatientPhno(), rx.getId());
        events.publish(new PaymentCompleted(po.getId(), po.getHospitalId(), po.getPatientPhno()));
    }

    private String requiredValue(Map<?, ?> medication, String... keys) {
        return value(medication, keys).filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Medication fields are required"));
    }

    private java.util.Optional<String> value(Map<?, ?> medication, String... keys) {
        for (String key : keys) {
            Object value = medication.get(key);
            if (value != null) return java.util.Optional.of(String.valueOf(value).trim());
        }
        return java.util.Optional.empty();
    }
}
