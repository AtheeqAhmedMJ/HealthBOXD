package com.healthbox.hms_backend.modules.prescriptions;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.patients.Patient;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.modules.appointments.Appointment;
import com.healthbox.hms_backend.modules.appointments.AppointmentRepository;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

// Read-only: prescriptions are written only by ConsultationMaterializer after a paid checkout.
@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepo;
    private final PatientRepository patientRepo;
    private final CurrentUser currentUser;
    private final AppointmentRepository appointmentRepo;

    public PrescriptionService(PrescriptionRepository prescriptionRepo, PatientRepository patientRepo, CurrentUser currentUser,
                               AppointmentRepository appointmentRepo) {
        this.prescriptionRepo = prescriptionRepo;
        this.patientRepo = patientRepo;
        this.currentUser = currentUser;
        this.appointmentRepo = appointmentRepo;
    }

    public List<Prescription> getAll() {
        AppUserPrincipal me = currentUser.get();
        return switch (me.getRole()) {
            case ADMIN -> prescriptionRepo.findByHospitalIdAndDoctorPhno(me.getHospitalId(), me.getPhno());
            case PATIENT -> prescriptionRepo.findByPatientPhnoAndStatusOrderByCreatedAtDesc(me.getPhno(), "FINALIZED");
            case SUPER_ADMIN -> throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        };
    }

    public List<Prescription> getByPatientPhno(String phno) {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() == Role.SUPER_ADMIN) throw new AccessDeniedException("Use /api/superadmin endpoints instead");
        Patient p = patientRepo.findByPhno(phno).orElseThrow(() -> new RuntimeException("Patient not found"));
        if (!p.getHospitalId().equals(me.getHospitalId())) throw new AccessDeniedException("Cross-tenant access denied");
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(phno)) throw new AccessDeniedException("Not your own record");
        return prescriptionRepo.findByPatientPhnoAndStatusOrderByCreatedAtDesc(phno, "FINALIZED");
    }

    public List<Prescription> getByAppointmentId(Long id) {
        AppUserPrincipal me = currentUser.get();
        Appointment appointment = appointmentRepo.findById(id).orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(appointment.getPatientPhno())) {
            throw new AccessDeniedException("Not your own record");
        }
        if (me.getRole() == Role.ADMIN && (!me.getHospitalId().equals(appointment.getHospitalId()) ||
                !me.getPhno().equals(appointment.getDoctorPhno()))) {
            throw new AccessDeniedException("Not authorized for this appointment");
        }
        return prescriptionRepo.findByAppointmentId(id).stream()
                .filter(prescription -> "FINALIZED".equals(prescription.getStatus())).toList();
    }
}
