package com.healthbox.hms_backend.modules.dashboard;

import com.healthbox.hms_backend.modules.appointments.Appointment;
import com.healthbox.hms_backend.modules.appointments.AppointmentRepository;
import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.patients.PatientRepository;
import com.healthbox.hms_backend.modules.payments.PaymentOrder;
import com.healthbox.hms_backend.modules.payments.PaymentOrderRepository;
import com.healthbox.hms_backend.modules.prescriptions.Prescription;
import com.healthbox.hms_backend.modules.prescriptions.PrescriptionRepository;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AppointmentRepository appointmentRepo;
    private final PatientRepository patientRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final PaymentOrderRepository paymentOrderRepo;
    private final CurrentUser currentUser;

    public DashboardController(AppointmentRepository appointmentRepo, PatientRepository patientRepo,
                                PrescriptionRepository prescriptionRepo, PaymentOrderRepository paymentOrderRepo,
                                CurrentUser currentUser) {
        this.appointmentRepo = appointmentRepo;
        this.patientRepo = patientRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.paymentOrderRepo = paymentOrderRepo;
        this.currentUser = currentUser;
    }

    @GetMapping("/patient-summary")
    public Map<String, Object> getPatientSummary() {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Patients only — admins use /api/dashboard/summary");
        }
        LocalDate today = LocalDate.now();
        List<Appointment> myAppointments = appointmentRepo.findByPatientPhno(me.getPhno());
        List<PaymentOrder> myPayments = paymentOrderRepo.findByPatientPhno(me.getPhno());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hasProfile", patientRepo.findByPhno(me.getPhno()).isPresent());
        summary.put("upcomingAppointments", myAppointments.stream().filter(a -> !a.getDate().isBefore(today)).count());
        summary.put("totalPrescriptions", prescriptionRepo.findByPatientPhno(me.getPhno()).size());
        summary.put("pendingPayments", myPayments.stream().filter(p -> "CREATED".equals(p.getStatus())).count());
        summary.put("totalPaidPayments", myPayments.stream().filter(p -> "SUCCESS".equals(p.getStatus()) || "PAID".equals(p.getStatus())).count());
        myAppointments.stream().filter(a -> !a.getDate().isBefore(today))
            .min(Comparator.comparing(Appointment::getDate).thenComparing(Appointment::getAppointmentTime,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .ifPresent(appointment -> summary.put("nextAppointment", Map.of(
                "id", appointment.getId(), "date", appointment.getDate(),
                "time", appointment.getAppointmentTime() == null ? "" : appointment.getAppointmentTime(),
                "status", appointment.getStatus(), "doctorPhno", Objects.toString(appointment.getDoctorPhno(), ""))));
        summary.put("recentAppointmentStatus", myAppointments.stream().findFirst().map(Appointment::getStatus).orElse(null));
        return summary;
    }

    @GetMapping("/summary")
    public Map<String, Object> getDashboardSummary() {
        var me = currentUser.get();
        if (me.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Admins only — patients use /api/dashboard/patient-summary, super admin uses /api/superadmin/stats");
        }
        Long hospitalId = me.getHospitalId();
        if (hospitalId == null) {
            throw new AccessDeniedException("Admin account is not assigned to a hospital");
        }
        Map<String, Object> summary = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        List<Appointment> appointments = appointmentRepo.findByHospitalId(hospitalId);
        List<Prescription> prescriptions = prescriptionRepo.findByHospitalId(hospitalId);
        long totalPatients = patientRepo.findByHospitalId(hospitalId).size();

        long todaysAppointments = appointments.stream().filter(a -> today.equals(a.getDate())).count();
        long monthlyAppointments = appointments.stream()
            .filter(a -> a.getDate() != null && thisMonth.equals(YearMonth.from(a.getDate())))
            .count();

        Map<String, Long> appointmentsPerPatient = appointments.stream()
            .filter(a -> a.getPatientPhno() != null)
            .collect(Collectors.groupingBy(Appointment::getPatientPhno, Collectors.counting()));
        long recurringPatients = appointmentsPerPatient.values().stream().filter(c -> c > 1).count();
        double recurringPercentage = totalPatients == 0 ? 0 : (recurringPatients * 100.0 / totalPatients);

        long prescriptionsThisMonth = prescriptions.stream()
                .filter(p -> p.getDate() != null && thisMonth.equals(YearMonth.from(p.getDate()))).count();
        long paidRevenuePaise = paymentOrderRepo.findByHospitalId(hospitalId).stream()
            .filter(p -> "SUCCESS".equals(p.getStatus()) || "PAID".equals(p.getStatus()))
            .filter(p -> p.getAmountPaise() != null)
            .mapToLong(PaymentOrder::getAmountPaise).sum();
        Map<String, Long> doctorEarnings = paymentOrderRepo.findByHospitalId(hospitalId).stream()
            .filter(p -> "SUCCESS".equals(p.getStatus()) || "PAID".equals(p.getStatus()))
            .filter(p -> p.getDoctorPhno() != null && p.getAmountPaise() != null)
            .collect(Collectors.groupingBy(PaymentOrder::getDoctorPhno, TreeMap::new,
                Collectors.summingLong(PaymentOrder::getAmountPaise)));

        LocalDate weekStart = today.minusDays(6);
        Map<String, Long> appointmentsByDay = appointments.stream()
                .filter(a -> a.getDate() != null && !a.getDate().isBefore(weekStart))
                .collect(Collectors.groupingBy(a -> a.getDate().toString(), TreeMap::new, Collectors.counting()));
        for (int i = 0; i < 7; i++) {
            appointmentsByDay.putIfAbsent(weekStart.plusDays(i).toString(), 0L);
        }

        summary.put("todaysAppointments", todaysAppointments);
        summary.put("totalPatients", totalPatients);
        summary.put("monthlyAppointments", monthlyAppointments);
        summary.put("recurringPatientsPercentage", String.format("%.1f", recurringPercentage));
        summary.put("prescriptionsThisMonth", prescriptionsThisMonth);
        summary.put("paidRevenuePaise", paidRevenuePaise);
        summary.put("doctorEarnings", doctorEarnings);
        summary.put("graphData", appointmentsByDay);

        return summary;
    }
}
