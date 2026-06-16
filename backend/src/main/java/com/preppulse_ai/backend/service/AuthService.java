package com.preppulse_ai.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.preppulse_ai.backend.dto.*;
import com.preppulse_ai.backend.entity.PasswordResetOtp;
import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.enums.AuthProvider;
import com.preppulse_ai.backend.repository.PasswordResetOtpRepository;
import com.preppulse_ai.backend.repository.UserRepository;
import com.preppulse_ai.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("An account with this email already exists.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .onboardingCompleted(false)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .user(convertToDto(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password."));

        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw new RuntimeException("This account was created using Google Sign-In. Please use the 'Continue with Google' option.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password.");
        }

        log.info("User logged in successfully: {}", user.getEmail());
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(convertToDto(user))
                .build();
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(request.getIdToken());
        if (payload == null) {
            throw new RuntimeException("Google ID Token verification failed.");
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Auto-create Google User
            user = User.builder()
                    .name(name != null ? name : "Google User")
                    .email(email)
                    .passwordHash(null)
                    .provider(AuthProvider.GOOGLE)
                    .profilePicture(pictureUrl)
                    .onboardingCompleted(false)
                    .build();

            userRepository.save(user);
            log.info("Auto-created Google user account for email: {}", email);
        } else {
            // Update profile picture if it changed
            if (pictureUrl != null && !pictureUrl.equals(user.getProfilePicture())) {
                user.setProfilePicture(pictureUrl);
                userRepository.save(user);
            }
            log.info("Google user logged in successfully: {}", email);
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(convertToDto(user))
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account registered with this email address."));

        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw new RuntimeException("This account is registered via Google Sign-In. Passwords cannot be reset for Google users.");
        }

        // Generate 6-digit random code
        String otpCode = String.format("%06d", new Random().nextInt(1000000));
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(10); // Valid for 10 minutes

        PasswordResetOtp otp = PasswordResetOtp.builder()
                .email(user.getEmail())
                .otpCode(otpCode)
                .expiryTime(expiryTime)
                .used(false)
                .build();

        passwordResetOtpRepository.save(otp);
        log.info("Generated OTP {} for user {}", otpCode, user.getEmail());

        // Send email
        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetOtp otp = passwordResetOtpRepository
                .findFirstByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(request.getEmail(), request.getOtpCode())
                .orElseThrow(() -> new RuntimeException("Invalid OTP code or email address."));

        if (otp.getExpiryTime().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("The OTP code has expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found."));

        // Update password hash
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // Mark OTP as used
        otp.setUsed(true);
        passwordResetOtpRepository.save(otp);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return convertToDto(user);
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .provider(user.getProvider().name())
                .onboardingCompleted(user.isOnboardingCompleted())
                .build();
    }
}
