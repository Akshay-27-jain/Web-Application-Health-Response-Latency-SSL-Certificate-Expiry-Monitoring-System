package com.uptimepulse.web.controller;

import com.uptimepulse.application.service.WebhookAlertService;
import com.uptimepulse.domain.enums.AlertChannelType;
import com.uptimepulse.domain.model.NotificationConfig;
import com.uptimepulse.domain.model.User;
import com.uptimepulse.infrastructure.persistence.NotificationConfigRepository;
import com.uptimepulse.infrastructure.persistence.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Manage alert webhooks, Slack, and Discord integrations")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationConfigRepository repository;
    private final UserRepository userRepository;
    private final WebhookAlertService webhookAlertService;

    public NotificationController(NotificationConfigRepository repository, UserRepository userRepository, WebhookAlertService webhookAlertService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.webhookAlertService = webhookAlertService;
    }

    @GetMapping
    @Operation(summary = "Get user's configured notification webhooks (Slack/Discord/Generic)")
    public ResponseEntity<List<NotificationConfig>> getConfigs(@AuthenticationPrincipal Object principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(repository.findByUserId(user.getId()));
    }

    @PostMapping
    @Operation(summary = "Create a new Webhook, Slack, or Discord notification channel")
    public ResponseEntity<NotificationConfig> createConfig(@AuthenticationPrincipal Object principal,
                                                           @RequestBody Map<String, String> body) {
        User user = resolveUser(principal);
        String name = body.getOrDefault("name", "Alert Webhook");
        String channelTypeStr = body.getOrDefault("channelType", "WEBHOOK");
        String webhookUrl = body.get("webhookUrl");

        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("Webhook URL is required");
        }

        AlertChannelType channelType;
        try {
            channelType = AlertChannelType.valueOf(channelTypeStr.toUpperCase());
        } catch (Exception e) {
            channelType = AlertChannelType.WEBHOOK;
        }

        NotificationConfig config = new NotificationConfig(user.getId(), name, channelType, webhookUrl);
        NotificationConfig saved = repository.save(config);

        // Send a test alert
        webhookAlertService.dispatchAlert(channelType, webhookUrl, "UptimePulse Integration Test", "https://uptimepulse.com", "Test alert successfully delivered!", "TEST");

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification channel config")
    public ResponseEntity<Map<String, String>> deleteConfig(@AuthenticationPrincipal Object principal, @PathVariable Long id) {
        User user = resolveUser(principal);
        NotificationConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification config not found: " + id));

        if (!config.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Notification channel deleted"));
    }

    private User resolveUser(Object principal) {
        if (principal instanceof User domainUser) {
            return domainUser;
        }
        if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
            return userRepository.findByEmail(springUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + springUser.getUsername()));
        }
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
        }
        throw new IllegalArgumentException("User unauthenticated");
    }
}
