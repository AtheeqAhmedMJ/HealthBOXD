package com.healthbox.hms_backend.modules.scheduling;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.healthbox.hms_backend.modules.tenant.Hospital;

@RestController
@RequestMapping("/api/schedule")
public class DoctorSlotController {

    private final DoctorSlotService service;

    public DoctorSlotController(DoctorSlotService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DoctorSlot> create(@RequestBody DoctorSlot s) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(s));
    }

    @GetMapping
    public List<DoctorSlot> getAll() {
        return service.getAll();
    }

    @GetMapping("/clinics")
    public List<Hospital> getClinics() {
        return service.getClinics();
    }

    @GetMapping("/doctor/{phno}")
    public List<DoctorSlot> getForDoctor(@PathVariable String phno) {
        return service.getForDoctor(phno);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
