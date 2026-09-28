package com.healthbox.hms_backend.modules.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findFirstByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String email, String purpose);
    Optional<OtpToken> findFirstByPhoneNumberAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String phoneNumber, String purpose);
}
