package com.healthbox.hms_backend.modules.fees;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceFeeBracketService {
    private final ServiceFeeBracketRepository repository;

    public ServiceFeeBracketService(ServiceFeeBracketRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceFeeBracket> all(Long hospitalId) {
        return hospitalId == null ? repository.findAllByOrderByMinAmountPaiseAsc() : repository.findByHospitalIdOrderByMinAmountPaiseAsc(hospitalId);
    }

    @Transactional(readOnly = true)
    public long calculate(long amountPaise, long fallbackPaise, Long hospitalId) {
        List<ServiceFeeBracket> brackets = hospitalId == null ? List.of() : repository.findByHospitalIdAndActiveTrueOrderByMinAmountPaiseAsc(hospitalId);
        if (brackets.isEmpty()) brackets = repository.findByHospitalIdIsNullAndActiveTrueOrderByMinAmountPaiseAsc();
        return brackets.stream()
                .filter(bracket -> amountPaise >= bracket.getMinAmountPaise())
                .filter(bracket -> bracket.getMaxAmountPaise() == null || amountPaise <= bracket.getMaxAmountPaise())
                .findFirst()
                .map(bracket -> "PERCENTAGE".equals(bracket.getFeeType())
                        ? Math.round((amountPaise * bracket.getFeeValue()) / 10_000.0)
                        : bracket.getFeeValue())
                .orElse(fallbackPaise);
    }

    @Transactional
    public ServiceFeeBracket create(ServiceFeeBracket input) {
        validate(input, null);
        input.setId(null);
        input.setActive(true);
        return repository.save(input);
    }

    @Transactional
    public ServiceFeeBracket update(Long id, ServiceFeeBracket input) {
        ServiceFeeBracket current = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service fee bracket not found"));
        validate(input, id);
        current.setMinAmountPaise(input.getMinAmountPaise());
        current.setHospitalId(input.getHospitalId());
        current.setMaxAmountPaise(input.getMaxAmountPaise());
        current.setFeeType(input.getFeeType());
        current.setFeeValue(input.getFeeValue());
        current.setActive(input.isActive());
        current.setUpdatedAt(java.time.LocalDateTime.now());
        return repository.save(current);
    }

    @Transactional
    public void deactivate(Long id) {
        ServiceFeeBracket current = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service fee bracket not found"));
        current.setActive(false);
        current.setUpdatedAt(java.time.LocalDateTime.now());
        repository.save(current);
    }

    private void validate(ServiceFeeBracket bracket, Long id) {
        if (bracket.getMinAmountPaise() == null || bracket.getMinAmountPaise() < 0) {
            throw new IllegalArgumentException("Minimum amount must be zero or greater");
        }
        if (bracket.getMaxAmountPaise() != null && bracket.getMaxAmountPaise() < bracket.getMinAmountPaise()) {
            throw new IllegalArgumentException("Maximum amount must be greater than or equal to minimum amount");
        }
        if (!"FIXED".equals(bracket.getFeeType()) && !"PERCENTAGE".equals(bracket.getFeeType())) {
            throw new IllegalArgumentException("Fee type must be FIXED or PERCENTAGE");
        }
        if (bracket.getFeeValue() == null || bracket.getFeeValue() < 0 || ("PERCENTAGE".equals(bracket.getFeeType()) && bracket.getFeeValue() > 10_000)) {
            throw new IllegalArgumentException("Fee value is invalid");
        }
        repository.findByHospitalIdOrderByMinAmountPaiseAsc(bracket.getHospitalId()).stream()
                .filter(existing -> !existing.getId().equals(id) && existing.isActive())
                .filter(existing -> rangesOverlap(existing, bracket))
                .findAny()
                .ifPresent(existing -> { throw new IllegalArgumentException("Service fee ranges cannot overlap"); });
    }

    private boolean rangesOverlap(ServiceFeeBracket left, ServiceFeeBracket right) {
        long leftMax = left.getMaxAmountPaise() == null ? Long.MAX_VALUE : left.getMaxAmountPaise();
        long rightMax = right.getMaxAmountPaise() == null ? Long.MAX_VALUE : right.getMaxAmountPaise();
        return left.getMinAmountPaise() <= rightMax && right.getMinAmountPaise() <= leftMax;
    }
}
