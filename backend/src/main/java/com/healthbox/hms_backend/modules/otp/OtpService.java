package com.healthbox.hms_backend.modules.otp;

import com.healthbox.hms_backend.modules.mail.EmailService;
import com.healthbox.hms_backend.modules.notifications.SmsProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class OtpService {

    private static final int TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private final OtpRepository repo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SmsProvider smsProvider;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRepository repo, EmailService emailService, PasswordEncoder passwordEncoder, SmsProvider smsProvider) {
        this.repo = repo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.smsProvider = smsProvider;
    }

    public void requestOtp(String email, String purpose) {
        requestOtp(null, email, purpose);
    }

    public void requestPhoneOtp(String phoneNumber, String fallbackEmail, String purpose) {
        requestOtp(phoneNumber, fallbackEmail, purpose);
    }

    private void requestOtp(String phoneNumber, String email, String purpose) {
        if ((phoneNumber == null || phoneNumber.isBlank()) && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("A phone number or email address is required");
        }
        String normalizedPhone = normalizePhone(phoneNumber);
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        var active = normalizedPhone == null
                ? repo.findFirstByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(normalizedEmail, purpose)
                : repo.findFirstByPhoneNumberAndPurposeAndConsumedFalseOrderByCreatedAtDesc(normalizedPhone, purpose);
        if (active.isPresent() && active.get().getCreatedAt().plusSeconds(RESEND_INTERVAL_SECONDS).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }
        active.ifPresent(token -> {
            token.setConsumed(true);
            repo.save(token);
        });
        String otp = String.format("%06d", random.nextInt(1_000_000));

        OtpToken token = new OtpToken();
        token.setEmail(normalizedEmail == null ? normalizedPhone + "@phone.invalid" : normalizedEmail);
        token.setPhoneNumber(normalizedPhone);
        token.setPurpose(purpose);
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));
        token.setChannel(normalizedPhone == null ? "EMAIL" : "PHONE");
        repo.save(token);

        if (normalizedPhone != null) {
            var smsResult = smsProvider.send(normalizedPhone,
                    "Your HealthBoxD verification code is " + otp + ". It expires in " + TTL_MINUTES + " minutes.");
            if (smsResult.delivered()) return;
            if (normalizedEmail != null && !normalizedEmail.endsWith("@phone.invalid")) {
                emailService.send(normalizedEmail, "Your HealthBoxD verification code",
                        "Your OTP is " + otp + ". It expires in " + TTL_MINUTES + " minutes. Do not share this code.");
            }
        } else {
            emailService.send(normalizedEmail, "Your HealthBoxD verification code",
                "Your OTP is " + otp + ". It expires in " + TTL_MINUTES + " minutes. Do not share this code.");
        }
    }

    public void verifyOtp(String email, String purpose, String otp) {
        verifyOtpInternal(repo.findFirstByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(email, purpose), otp);
    }

    public void verifyPhoneOtp(String phoneNumber, String purpose, String otp) {
        verifyOtpInternal(repo.findFirstByPhoneNumberAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                normalizePhone(phoneNumber), purpose), otp);
    }

    private void verifyOtpInternal(java.util.Optional<OtpToken> candidate, String otp) {
        OtpToken token = candidate
                .orElseThrow(() -> new IllegalArgumentException("No OTP requested for this email"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP expired, please request a new one");
        }
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Too many invalid OTP attempts, please request a new one");
        }
        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            token.setAttempts(token.getAttempts() + 1);
            token.setLastAttemptAt(LocalDateTime.now());
            repo.save(token);
            throw new IllegalArgumentException("Incorrect OTP");
        }
        token.setConsumed(true);
        repo.save(token);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return null;
        String normalized = phoneNumber.replaceAll("[^0-9+]", "");
        if (!normalized.matches("^\\+?[1-9][0-9]{7,14}$")) {
            throw new IllegalArgumentException("Invalid phone number");
        }
        return normalized;
    }
}
