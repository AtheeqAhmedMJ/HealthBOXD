package com.healthbox.hms_backend.modules.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

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
    long countByHospitalIdAndStatusIn(Long hospitalId, List<String> statuses);
    @Query("select coalesce(sum(p.amountPaise), 0) from PaymentOrder p where p.hospitalId = :hospitalId and p.status in :statuses")
    long sumAmountByHospitalAndStatuses(@Param("hospitalId") Long hospitalId, @Param("statuses") List<String> statuses);
    @Query("select coalesce(sum(p.platformFeePaise), 0) from PaymentOrder p where p.hospitalId = :hospitalId and p.status in :statuses")
    long sumPlatformFeeByHospitalAndStatuses(@Param("hospitalId") Long hospitalId, @Param("statuses") List<String> statuses);
    long countByHospitalIdAndStatusInAndPaidAtBetween(Long hospitalId, List<String> statuses, LocalDateTime from, LocalDateTime to);
    long countByHospitalId(Long hospitalId);
    @Query("select p.doctorPhno, sum(p.amountPaise) from PaymentOrder p where p.hospitalId = :hospitalId and p.status in :statuses and p.doctorPhno is not null group by p.doctorPhno order by p.doctorPhno")
    List<Object[]> sumByDoctorAndHospital(@Param("hospitalId") Long hospitalId, @Param("statuses") List<String> statuses);
    @Query("select count(p) from PaymentOrder p where p.status in :statuses")
    long countByStatuses(@Param("statuses") List<String> statuses);
    @Query("select coalesce(sum(p.amountPaise), 0) from PaymentOrder p where p.status in :statuses")
    long sumAmountByStatuses(@Param("statuses") List<String> statuses);
    @Query("select coalesce(sum(p.platformFeePaise), 0) from PaymentOrder p where p.status in :statuses")
    long sumPlatformFeeByStatuses(@Param("statuses") List<String> statuses);
    @Query("select count(p) from PaymentOrder p where p.status in :statuses and p.paidAt between :from and :to")
    long countPaidBetween(@Param("statuses") List<String> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select p.hospitalId, count(p), coalesce(sum(p.platformFeePaise), 0) from PaymentOrder p where p.status in :statuses group by p.hospitalId")
    List<Object[]> summarizeByHospital(@Param("statuses") List<String> statuses);
}
