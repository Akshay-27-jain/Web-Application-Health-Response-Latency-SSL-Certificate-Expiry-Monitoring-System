package com.uptimepulse.config;

import com.uptimepulse.domain.enums.MonitorStatus;
import com.uptimepulse.domain.enums.Role;
import com.uptimepulse.domain.model.Monitor;
import com.uptimepulse.domain.model.User;
import com.uptimepulse.infrastructure.persistence.MonitorRepository;
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
    private final MonitorRepository monitorRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           MonitorRepository monitorRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.monitorRepository = monitorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("[DATA INITIALIZER] Checking initial database state...");

        User demoUser = userRepository.findByEmail("user@uptimepulse.com")
                .orElseGet(() -> userRepository.save(new User("user@uptimepulse.com", passwordEncoder.encode("password123"), "Demo Developer", Role.USER)));

        User adminUser = userRepository.findByEmail("admin@uptimepulse.com")
                .orElseGet(() -> userRepository.save(new User("admin@uptimepulse.com", passwordEncoder.encode("admin123"), "System Administrator", Role.ADMIN)));

        if (monitorRepository.count() == 0) {
            log.info("[DATA INITIALIZER] Seeding sample monitors for Demo User & Admin...");

            createSeedMonitor(demoUser.getId(), "Google Search Engine", "https://www.google.com", 1, "pub-google-search", 42, 120);
            createSeedMonitor(demoUser.getId(), "GitHub Core Platform", "https://github.com", 3, "pub-github-main", 115, 85);
            createSeedMonitor(demoUser.getId(), "Cloudflare Global DNS", "https://1.1.1.1", 5, "pub-cloudflare-dns", 18, 365);
            createSeedMonitor(demoUser.getId(), "Wikipedia Knowledge Base", "https://www.wikipedia.org", 3, "pub-wikipedia-org", 64, 190);
            createSeedMonitor(demoUser.getId(), "HTTPBin Test Endpoint", "https://httpbin.org/get", 1, "pub-httpbin-test", 88, 45);

            createSeedMonitor(adminUser.getId(), "Google Search Engine", "https://www.google.com", 1, "pub-admin-google", 42, 120);
            createSeedMonitor(adminUser.getId(), "GitHub Core Platform", "https://github.com", 3, "pub-admin-github", 115, 85);
            createSeedMonitor(adminUser.getId(), "Cloudflare Primary DNS", "https://1.1.1.1", 2, "pub-admin-dns", 15, 365);

            log.info("[DATA INITIALIZER] Sample monitors successfully seeded!");
        }
    }

    private void createSeedMonitor(Long userId, String name, String url, int interval, String publicId, long latency, int sslDays) {
        Monitor monitor = new Monitor(userId, name, url, interval);
        monitor.setPublicId(publicId);
        monitor.setStatus(MonitorStatus.UP);
        monitor.setLastLatencyMs(latency);
        monitor.setSslDaysRemaining(sslDays);
        monitorRepository.save(monitor);
    }
}
