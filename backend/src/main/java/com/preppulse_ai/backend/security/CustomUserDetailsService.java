package com.preppulse_ai.backend.security;

import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // For Google users, passwordHash is null. In Spring Security, password cannot be null.
        // We can supply an empty string or a dummy string for external users.
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "";

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
