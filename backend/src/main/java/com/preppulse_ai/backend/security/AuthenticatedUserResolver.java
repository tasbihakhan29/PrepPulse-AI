package com.preppulse_ai.backend.security;

import com.preppulse_ai.backend.entity.User;
import com.preppulse_ai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public User resolve(Principal principal) {
        if (principal == null) {
            throw new SecurityException("No authenticated principal found.");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + principal.getName()
                ));
    }
}
