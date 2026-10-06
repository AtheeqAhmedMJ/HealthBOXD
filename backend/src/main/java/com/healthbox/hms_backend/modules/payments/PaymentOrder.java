package com.healthbox.hms_backend.modules.payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

// Staging record: nothing clinical is persisted (Patient/Prescription/Billing)
// until this order's status flips to PAID via verified Razorpay callback/webhook.
@Entity
@Table(name = "payment_orders")
@Getter
@Setter
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "doctor_phno", nullable = false)
    private String doctorPhno;

    @Column(name = "patient_phno", nullable = false)
    private String patientPhno;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "consultation_id")
    private Long consultationId;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "razorpay_order_id", nullable = false, unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "platform_fee_paise", nullable = false)
    private Long platformFeePaise;

    @Column(name = "doctor_amount_paise", nullable = false)
    private Long doctorAmountPaise;

    @Column(name = "transfer_id")
    private String transferId;

    @Column(name = "transfer_status", nullable = false)
    private String transferStatus = "PENDING";

    @Column(name = "transfer_error")
    private String transferError;

    @Column(nullable = false)
    private String status = "CREATED"; // CREATED, PAID, FAILED

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason")
    private String failureReason;

    private LocalDateTime updatedAt = LocalDateTime.now();

    // holds the clinical/patient draft (symptoms, meds, vitals, remarks, patient demographics)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime paidAt;
}
