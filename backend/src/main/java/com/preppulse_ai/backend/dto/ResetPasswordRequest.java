package com.preppulse_ai.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be exactly 6 digits")
    private String otpCode;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;
}
