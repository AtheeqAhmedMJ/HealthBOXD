package com.healthbox.hms_backend.modules.payments;

import com.healthbox.hms_backend.modules.auth.User;
import com.healthbox.hms_backend.modules.auth.UserRepository;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class DoctorPayoutService {
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;

    public DoctorPayoutService(UserRepository userRepository, RazorpayClient razorpayClient) {
        this.userRepository = userRepository;
        this.razorpayClient = razorpayClient;
    }

    public void transfer(PaymentOrder order) {
        User doctor = userRepository.findById(order.getDoctorPhno()).orElse(null);
        if (doctor == null || doctor.getRazorpayAccountId() == null || doctor.getRazorpayAccountId().isBlank()) {
            order.setTransferStatus("PENDING_ACCOUNT");
            order.setTransferError("Doctor Razorpay linked account is not configured");
            return;
        }
        try {
            JSONObject transferRequest = new JSONObject();
            transferRequest.put("account", doctor.getRazorpayAccountId());
            transferRequest.put("amount", order.getDoctorAmountPaise());
            transferRequest.put("currency", "INR");
            transferRequest.put("on_hold", false);
            transferRequest.put("notes", new JSONObject().put("payment_order_id", order.getId()));
            var transfer = razorpayClient.transfers.create(transferRequest);
            order.setTransferId(transfer.get("id"));
            order.setTransferStatus("TRANSFERRED");
            order.setTransferError(null);
        } catch (Exception exception) {
            order.setTransferStatus("FAILED");
            order.setTransferError(exception.getMessage());
        }
    }
}