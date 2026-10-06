package com.healthbox.hms_backend.modules.fees;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceFeeBracketRepository extends JpaRepository<ServiceFeeBracket, Long> {
    List<ServiceFeeBracket> findAllByOrderByMinAmountPaiseAsc();
    List<ServiceFeeBracket> findByActiveTrueOrderByMinAmountPaiseAsc();
    List<ServiceFeeBracket> findByHospitalIdOrderByMinAmountPaiseAsc(Long hospitalId);
    List<ServiceFeeBracket> findByHospitalIdAndActiveTrueOrderByMinAmountPaiseAsc(Long hospitalId);
    List<ServiceFeeBracket> findByHospitalIdIsNullAndActiveTrueOrderByMinAmountPaiseAsc();
}
