package com.healthbox.hms_backend.modules.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);
    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentOrder p where p.id = :id")
    Optional<PaymentOrder> findByIdForUpdate(@Param("id") Long id);
    List<PaymentOrder> findByHospitalId(Long hospitalId);
    List<PaymentOrder> findByPatientPhno(String patientPhno);
    long countByStatus(String status);
    List<PaymentOrder> findByStatus(String status);
}
