package com.healthbox.hms_backend.modules.appointments;

import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientRepository patientRepository;
    @Mock CurrentUser currentUser;

    private AppointmentService service;

    @BeforeEach
    void setup() {
        service = new AppointmentService(appointmentRepository, patientRepository, currentUser);
        when(currentUser.get()).thenReturn(new AppUserPrincipal("9000000000", "doctor", Role.ADMIN, 1L));
    }

    @Test
    void returnsExistingAppointmentForRetryKey() {
        Appointment existing = appointment("retry-key");
        Appointment request = appointment("retry-key");
        when(appointmentRepository.findByIdempotencyKey("retry-key")).thenReturn(Optional.of(existing));
        assertSame(existing, service.create(request));
    }

    private Appointment appointment(String key) {
        Appointment appointment = new Appointment();
        appointment.setIdempotencyKey(key);
        appointment.setPatientPhno("9000000001");
        appointment.setDate(LocalDate.now());
        appointment.setAppointmentTime(LocalTime.NOON);
        return appointment;
    }
}
