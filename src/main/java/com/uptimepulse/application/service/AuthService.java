package com.uptimepulse.application.service;

import com.uptimepulse.domain.enums.Role;
import com.uptimepulse.domain.model.User;
import com.uptimepulse.infrastructure.persistence.UserRepository;
import com.uptimepulse.web.dto.AuthResponse;
import com.uptimepulse.web.dto.LoginRequest;
import com.uptimepulse.web.dto.RegisterRequest;
import com.uptimepulse.web.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already registered: " + normalizedEmail);
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.getPassword().trim()),
                request.getFullName().trim(),
                Role.USER
        );

        User saved = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(saved.getEmail(), saved.getId(), saved.getRole().name());

        return new AuthResponse(token, saved.getEmail(), saved.getFullName(), saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
