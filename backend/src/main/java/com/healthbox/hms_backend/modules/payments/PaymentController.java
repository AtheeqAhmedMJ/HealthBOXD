package com.healthbox.hms_backend.modules.payments;

import com.healthbox.hms_backend.modules.payments.dto.CheckoutRequest;
import com.healthbox.hms_backend.modules.payments.dto.CheckoutResponse;
import com.healthbox.hms_backend.modules.payments.dto.VerifyRequest;
import com.healthbox.hms_backend.modules.payments.dto.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCheckout(req));
    }

    // called by the frontend right after Razorpay Checkout succeeds (fast path; webhook is the durable backup)
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@RequestBody VerifyRequest req) {
        service.verifyAndCapture(req);
        return ResponseEntity.ok(Map.of("status", "PAID"));
    }

    // Razorpay server-to-server callback — no JWT, verified purely by signature
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawPayload,
                                         @RequestHeader("X-Razorpay-Signature") String signature) {
        service.handleWebhook(rawPayload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<PaymentResponse> getAll() {
        return service.getAll().stream().map(this::response).toList();
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return response(service.getById(id));
    }

    private PaymentResponse response(PaymentOrder payment) {
        return new PaymentResponse(payment.getId(), payment.getRazorpayOrderId(), payment.getRazorpayPaymentId(),
                payment.getAppointmentId(), payment.getConsultationId(), payment.getPrescriptionId(),
                payment.getAmountPaise(), payment.getStatus(), payment.getCreatedAt(), payment.getPaidAt());
    }
}
