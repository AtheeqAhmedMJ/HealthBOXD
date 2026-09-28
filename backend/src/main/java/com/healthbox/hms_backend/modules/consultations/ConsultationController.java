package com.healthbox.hms_backend.modules.consultations;

import com.healthbox.hms_backend.modules.consultations.dto.ConsultationRequest;
import com.healthbox.hms_backend.modules.consultations.dto.ConsultationResponse;
import com.healthbox.hms_backend.modules.consultations.dto.ConsultationUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {
    private final ConsultationService service;

    public ConsultationController(ConsultationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConsultationResponse> create(@Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ConsultationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public ConsultationResponse update(@PathVariable Long id, @Valid @RequestBody ConsultationUpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/start")
    public ConsultationResponse start(@PathVariable Long id) {
        return service.transition(id, "IN_PROGRESS");
    }

    @PostMapping("/{id}/await-payment")
    public ConsultationResponse awaitPayment(@PathVariable Long id) {
        return service.transition(id, "AWAITING_PAYMENT");
    }

    @GetMapping("/patient/me")
    public List<ConsultationResponse> patientRecords() {
        return service.patientRecords();
    }

    @GetMapping("/patient/{phno}")
    public List<ConsultationResponse> recordsForPatient(@PathVariable String phno) {
        return service.recordsForPatient(phno);
    }

    @GetMapping("/records")
    public List<ConsultationResponse> recordsForDoctor() {
        return service.recordsForDoctor();
    }
}
