package com.uptimepulse.config;

import com.uptimepulse.domain.enums.Role;
import com.uptimepulse.domain.model.User;
import com.uptimepulse.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("[DATA INITIALIZER] Checking initial database state...");

        User demoUser = userRepository.findByEmail("user@uptimepulse.com")
                .orElseGet(() -> userRepository.save(new User("user@uptimepulse.com", passwordEncoder.encode("password123"), "Demo Developer", Role.USER)));

        User adminUser = userRepository.findByEmail("admin@uptimepulse.com")
                .orElseGet(() -> userRepository.save(new User("admin@uptimepulse.com", passwordEncoder.encode("admin123"), "System Administrator", Role.ADMIN)));

        log.info("[DATA INITIALIZER] Initial database verified. Users initialized (demo: user@uptimepulse.com, admin: admin@uptimepulse.com). No auto-seeded monitors.");
    }
}
