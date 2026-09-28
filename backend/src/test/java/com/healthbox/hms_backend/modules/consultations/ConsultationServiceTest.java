package com.healthbox.hms_backend.modules.consultations;

import com.healthbox.hms_backend.modules.appointments.AppointmentRepository;
import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.patients.Patient;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.modules.prescriptions.PrescriptionRepository;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import com.healthbox.hms_backend.modules.consultations.dto.ConsultationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {
    @Mock ConsultationRepository repository;
    @Mock PatientRepository patientRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock PrescriptionRepository prescriptionRepository;
    @Mock CurrentUser currentUser;

    private ConsultationService service;
    private final AppUserPrincipal doctor = new AppUserPrincipal("9000000000", "doctor", Role.ADMIN, 1L);

    @BeforeEach
    void setUp() {
        service = new ConsultationService(repository, patientRepository, appointmentRepository, prescriptionRepository, currentUser);
        when(currentUser.get()).thenReturn(doctor);
    }

    @Test
    void createsDraftForKnownPatient() {
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setPhno("9000000001");
        patient.setHospitalId(1L);
        when(patientRepository.findByPhno("9000000001")).thenReturn(Optional.of(patient));
        when(repository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationRequest request = new ConsultationRequest();
        request.setPatientPhno("9000000001");
        request.setSymptoms("fever");

        var response = service.create(request);
        assertEquals("CREATED", response.status());
        assertEquals("9000000001", response.patientPhno());
    }

    @Test
    void rejectsInvalidTransition() {
        Consultation consultation = new Consultation();
        consultation.setId(4L);
        consultation.setPatientPhno("9000000001");
        consultation.setDoctorPhno(doctor.getPhno());
        consultation.setStatus("CREATED");
        when(repository.findById(4L)).thenReturn(Optional.of(consultation));

        assertThrows(IllegalStateException.class, () -> service.transition(4L, "FINALIZED"));
    }

    @Test
    void rejectsDifferentDoctor() {
        Consultation consultation = new Consultation();
        consultation.setId(5L);
        consultation.setPatientPhno("9000000001");
        consultation.setDoctorPhno("9111111111");
        consultation.setStatus("IN_PROGRESS");
        when(repository.findById(5L)).thenReturn(Optional.of(consultation));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.get(5L));
    }
}
