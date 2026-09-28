package com.healthbox.hms_backend.modules.approvals;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<ApprovalRequest> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);
}
