package com.healthbox.hms_backend.modules.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.healthbox.hms_backend.modules.auth.Role;
import com.healthbox.hms_backend.modules.appointments.AppointmentRepository;
import com.healthbox.hms_backend.modules.charges.ChargeItem;
import com.healthbox.hms_backend.modules.charges.ChargeItemRepository;
import com.healthbox.hms_backend.modules.payments.dto.CheckoutRequest;
import com.healthbox.hms_backend.modules.payments.dto.CheckoutResponse;
import com.healthbox.hms_backend.modules.payments.dto.VerifyRequest;
import com.healthbox.hms_backend.security.principal.AppUserPrincipal;
import com.healthbox.hms_backend.security.principal.CurrentUser;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The payment gate: nothing clinical (Patient profile, Prescription, Billing) is written
 * until a Razorpay payment for that consultation is verified as captured.
 */
@Service
public class PaymentService {

    private final PaymentOrderRepository orderRepo;
    private final ChargeItemRepository chargeItemRepo;
    private final AppointmentRepository appointmentRepo;
    private final RazorpayClient razorpayClient;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;
    private final ConsultationMaterializer materializer;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Value("${platform.fee-paise:75}")
    private long platformFeePaise;

    public PaymentService(PaymentOrderRepository orderRepo, ChargeItemRepository chargeItemRepo,
                           AppointmentRepository appointmentRepo, RazorpayClient razorpayClient,
                           CurrentUser currentUser, ObjectMapper objectMapper, ConsultationMaterializer materializer) {
        this.orderRepo = orderRepo;
        this.chargeItemRepo = chargeItemRepo;
        this.appointmentRepo = appointmentRepo;
        this.razorpayClient = razorpayClient;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.materializer = materializer;
    }

    public CheckoutResponse createCheckout(CheckoutRequest req) {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the doctor (admin) can start a checkout");
        }
        if (req.getPatientPhno() == null || req.getPatientName() == null) {
            throw new IllegalArgumentException("Patient phone number and name are required");
        }
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            var existing = orderRepo.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) {
                PaymentOrder po = existing.get();
                return new CheckoutResponse(po.getId(), po.getRazorpayOrderId(), po.getAmountPaise(), "INR", keyId);
            }
        }
        if (req.getAppointmentId() != null) {
            var appt = appointmentRepo.findById(req.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            if (!appt.getHospitalId().equals(me.getHospitalId()) || !appt.getPatientPhno().equals(req.getPatientPhno())) {
                throw new AccessDeniedException("Appointment does not belong to this patient/hospital");
            }
        }

        long amountPaise;
        if (req.getChargeItemIds() != null && !req.getChargeItemIds().isEmpty()) {
            amountPaise = 0;
            for (Long id : req.getChargeItemIds()) {
                ChargeItem item = chargeItemRepo.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Charge item not found: " + id));
                if (!item.getHospitalId().equals(me.getHospitalId())) {
                    throw new AccessDeniedException("Cross-tenant access denied");
                }
                amountPaise += item.getAmountPaise();
            }
        } else if (req.getCustomAmountPaise() != null) {
            amountPaise = req.getCustomAmountPaise();
        } else {
            throw new IllegalArgumentException("Provide chargeItemIds or a customAmountPaise");
        }

        if (amountPaise <= platformFeePaise) {
            throw new IllegalArgumentException("Charges must exceed the platform fee (₹" + (platformFeePaise / 100.0) + ")");
        }

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", UUID.randomUUID().toString());
            orderRequest.put("payment_capture", true);
            Order order = razorpayClient.orders.create(orderRequest);

            PaymentOrder po = new PaymentOrder();
            po.setHospitalId(me.getHospitalId());
            po.setDoctorPhno(me.getPhno());
            po.setPatientPhno(req.getPatientPhno());
            po.setAppointmentId(req.getAppointmentId());
            po.setConsultationId(req.getConsultationId());
            po.setPrescriptionId(req.getPrescriptionId());
            po.setRazorpayOrderId(order.get("id"));
            po.setAmountPaise(amountPaise);
            po.setPlatformFeePaise(platformFeePaise);
            po.setDoctorAmountPaise(amountPaise - platformFeePaise);
            po.setStatus("CREATED");
            po.setIdempotencyKey(req.getIdempotencyKey());
            po.setUpdatedAt(java.time.LocalDateTime.now());
            po.setPayload(objectMapper.convertValue(req, new TypeReference<Map<String, Object>>() {}));
            orderRepo.save(po);

            return new CheckoutResponse(po.getId(), po.getRazorpayOrderId(), amountPaise, "INR", keyId);
        } catch (RazorpayException e) {
            throw new RuntimeException("Could not create payment order: " + e.getMessage(), e);
        }
    }

    public void verifyAndCapture(VerifyRequest v) {
        PaymentOrder po = orderRepo.findByRazorpayOrderId(v.getRazorpayOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment order"));

        AppUserPrincipal actor = currentUser.get();
        if (actor.getRole() == Role.PATIENT && !actor.getPhno().equals(po.getPatientPhno())) {
            throw new AccessDeniedException("Payment does not belong to this patient");
        }
        if (actor.getRole() == Role.ADMIN && (!actor.getHospitalId().equals(po.getHospitalId()) || !actor.getPhno().equals(po.getDoctorPhno()))) {
            throw new AccessDeniedException("Payment does not belong to this clinic");
        }

        if ("SUCCESS".equals(po.getStatus()) || "PAID".equals(po.getStatus())) return;

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", v.getRazorpayOrderId());
        options.put("razorpay_payment_id", v.getRazorpayPaymentId());
        options.put("razorpay_signature", v.getRazorpaySignature());

        boolean valid;
        try {
            valid = Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            valid = false;
        }

        if (!valid) {
            po.setStatus("FAILED");
            po.setFailureReason("Payment signature verification failed");
            orderRepo.save(po);
            throw new AccessDeniedException("Payment signature verification failed");
        }

        try {
            var payment = razorpayClient.payments.fetch(v.getRazorpayPaymentId());
            if (!v.getRazorpayOrderId().equals(payment.get("order_id"))
                    || !"INR".equals(payment.get("currency"))
                    || ((Number) payment.get("amount")).longValue() != po.getAmountPaise()
                    || !"captured".equalsIgnoreCase(String.valueOf(payment.get("status")))) {
                po.setStatus("FAILED");
                po.setFailureReason("Payment amount, currency, order, or capture status did not match");
                orderRepo.save(po);
                throw new AccessDeniedException("Payment verification details did not match the order");
            }
        } catch (RazorpayException | ClassCastException e) {
            po.setStatus("FAILED");
            po.setFailureReason("Could not verify payment with provider");
            orderRepo.save(po);
            throw new IllegalArgumentException("Could not verify payment with provider");
        }

        materializer.materialize(po, v.getRazorpayPaymentId());
    }

    public void handleWebhook(String rawPayload, String signature) {
        boolean valid;
        try {
            valid = Utils.verifyWebhookSignature(rawPayload, signature, webhookSecret);
        } catch (RazorpayException e) {
            valid = false;
        }
        if (!valid) throw new AccessDeniedException("Invalid webhook signature");

        try {
            JSONObject event = new JSONObject(rawPayload);
            String eventType = event.optString("event");
            if (!"payment.captured".equals(eventType)) return;

            JSONObject payment = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
            String razorpayOrderId = payment.getString("order_id");
            String paymentId = payment.getString("id");

            orderRepo.findByRazorpayOrderId(razorpayOrderId).ifPresent(po -> {
                if (!"PAID".equals(po.getStatus())) {
                    materializer.materialize(po, paymentId);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Could not process webhook payload: " + e.getMessage(), e);
        }
    }

    public List<PaymentOrder> getAll() {
        AppUserPrincipal me = currentUser.get();
        if (me.getRole() == Role.PATIENT) return orderRepo.findByPatientPhno(me.getPhno());
        return orderRepo.findByHospitalId(me.getHospitalId());
    }

    public PaymentOrder getById(Long id) {
        AppUserPrincipal me = currentUser.get();
        PaymentOrder po = orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Payment not found"));
        if (me.getRole() == Role.PATIENT && !me.getPhno().equals(po.getPatientPhno())) {
            throw new AccessDeniedException("Not your own record");
        }
        if (me.getRole() == Role.ADMIN && !me.getHospitalId().equals(po.getHospitalId())) {
            throw new AccessDeniedException("Cross-tenant access denied");
        }
        return po;
    }
}
