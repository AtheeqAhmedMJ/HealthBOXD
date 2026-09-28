package com.healthbox.hms_backend.modules.medications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {
    private final MedicationRepository repository;

    public MedicationController(MedicationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<Medication> search(@RequestParam(defaultValue = "") String search,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByName(
                search.trim(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }
}
