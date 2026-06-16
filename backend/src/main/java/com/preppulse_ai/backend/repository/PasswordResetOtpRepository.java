package com.preppulse_ai.backend.repository;

import com.preppulse_ai.backend.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, UUID> {
    Optional<PasswordResetOtp> findFirstByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(String email, String otpCode);
}
