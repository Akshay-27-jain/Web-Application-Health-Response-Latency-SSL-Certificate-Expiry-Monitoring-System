package com.uptimepulse.application.service;

import com.uptimepulse.application.port.CachePort;
import com.uptimepulse.application.port.EventPublisherPort;
import com.uptimepulse.domain.enums.AlertChannelType;
import com.uptimepulse.domain.enums.MonitorStatus;
import com.uptimepulse.domain.model.AlertLog;
import com.uptimepulse.domain.model.Monitor;
import com.uptimepulse.domain.model.NotificationConfig;
import com.uptimepulse.domain.model.PingResult;
import com.uptimepulse.domain.model.User;
import com.uptimepulse.infrastructure.persistence.AlertLogRepository;
import com.uptimepulse.infrastructure.persistence.MonitorRepository;
import com.uptimepulse.infrastructure.persistence.NotificationConfigRepository;
import com.uptimepulse.infrastructure.persistence.PingResultRepository;
import com.uptimepulse.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final PingResultRepository pingResultRepository;
    private final AlertLogRepository alertLogRepository;
    private final NotificationConfigRepository notificationConfigRepository;
    private final UserRepository userRepository;
    private final HealthPingService healthPingService;
    private final WebhookAlertService webhookAlertService;
    private final EmailAlertService emailAlertService;
    private final CachePort cachePort;
    private final EventPublisherPort eventPublisherPort;

    public MonitorService(MonitorRepository monitorRepository,
                          PingResultRepository pingResultRepository,
                          AlertLogRepository alertLogRepository,
                          NotificationConfigRepository notificationConfigRepository,
                          UserRepository userRepository,
                          HealthPingService healthPingService,
                          WebhookAlertService webhookAlertService,
                          EmailAlertService emailAlertService,
                          CachePort cachePort,
                          EventPublisherPort eventPublisherPort) {
        this.monitorRepository = monitorRepository;
        this.pingResultRepository = pingResultRepository;
        this.alertLogRepository = alertLogRepository;
        this.notificationConfigRepository = notificationConfigRepository;
        this.userRepository = userRepository;
        this.healthPingService = healthPingService;
        this.webhookAlertService = webhookAlertService;
        this.emailAlertService = emailAlertService;
        this.cachePort = cachePort;
        this.eventPublisherPort = eventPublisherPort;
    }

    public List<Monitor> getMonitorsByUserId(Long userId) {
        return monitorRepository.findByUserId(userId);
    }

    public Optional<Monitor> getMonitorById(Long id) {
        return monitorRepository.findById(id);
    }

    public Optional<Monitor> getMonitorByPublicId(String publicId) {
        Object cached = cachePort.get("public_monitor:" + publicId);
        if (cached instanceof Monitor m) {
            return Optional.of(m);
        }
        Optional<Monitor> monitor = monitorRepository.findByPublicId(publicId);
        monitor.ifPresent(m -> cachePort.put("public_monitor:" + publicId, m, 300));
        return monitor;
    }

    @Transactional
    public Monitor createMonitor(Long userId, String name, String rawUrl, String monitorType, String tags, Integer interval) {
        String normalizedUrl = HealthPingService.normalizeUrl(rawUrl);
        Monitor monitor = new Monitor(userId, name, normalizedUrl, monitorType, tags, interval);
        Monitor saved = monitorRepository.save(monitor);
        triggerManualPing(saved.getId());
        return saved;
    }

    public PingResult quickScan(String rawUrl) {
        String normalizedUrl = HealthPingService.normalizeUrl(rawUrl);
        return healthPingService.pingUrl(0L, normalizedUrl);
    }

    @Transactional
    public void deleteMonitor(Long id) {
        monitorRepository.deleteById(id);
    }

    @Transactional
    public PingResult triggerManualPing(Long monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new RuntimeException("Monitor not found with ID: " + monitorId));

        PingResult result = healthPingService.pingUrl(monitor.getId(), monitor.getUrl(), monitor.getMonitorType());
        pingResultRepository.save(result);

        // Update Monitor Status & Stats
        monitor.setStatus(result.getStatus());
        monitor.setLastLatencyMs(result.getLatencyMs());
        monitor.setSslDaysRemaining(result.getSslDaysRemaining());

        if (!result.getStatus().equals(MonitorStatus.UP)) {
            int failures = monitor.getConsecutiveFailures() + 1;
            monitor.setConsecutiveFailures(failures);

            if (failures >= 1) { // Alert immediately on down/degraded
                String alertMsg = "Website " + monitor.getName() + " (" + monitor.getUrl() + ") is " + result.getStatus() + " (HTTP " + result.getStatusCode() + ")";
                eventPublisherPort.publishAlertEvent(monitor.getId(), alertMsg, result.getStatus().name());
                
                // Record internal Alert Log
                alertLogRepository.save(new AlertLog(monitor.getId(), monitor.getUserId(), alertMsg, AlertChannelType.LOG_ONLY, true));
                monitor.setLastAlertSentAt(LocalDateTime.now());

                // 1. Send HTML Email Alert to user email
                userRepository.findById(monitor.getUserId()).ifPresent(user -> {
                    emailAlertService.sendEmailAlert(user.getEmail(), monitor.getName(), monitor.getUrl(), result.getStatus().name(), alertMsg);
                    alertLogRepository.save(new AlertLog(monitor.getId(), monitor.getUserId(), alertMsg + " via EMAIL to " + user.getEmail(), AlertChannelType.EMAIL, true));
                });

                // 2. Dispatch to configured external Webhook/Slack/Discord channels
                List<NotificationConfig> configs = notificationConfigRepository.findByUserIdAndEnabledTrue(monitor.getUserId());
                for (NotificationConfig config : configs) {
                    webhookAlertService.dispatchAlert(
                            config.getChannelType(),
                            config.getWebhookUrl(),
                            monitor.getName(),
                            monitor.getUrl(),
                            alertMsg,
                            result.getStatus().name()
                    );
                    alertLogRepository.save(new AlertLog(monitor.getId(), monitor.getUserId(), alertMsg + " via " + config.getChannelType(), config.getChannelType(), true));
                }
            }
        } else {
            monitor.setConsecutiveFailures(0);
        }

        Monitor updated = monitorRepository.save(monitor);
        cachePort.put("public_monitor:" + updated.getPublicId(), updated, 300);

        return result;
    }

    public List<PingResult> getRecentPingResults(Long monitorId) {
        return pingResultRepository.findTop10ByMonitorIdOrderByTimestampDesc(monitorId);
    }

    public List<PingResult> getHistoricalPingResults(Long monitorId, int limit) {
        return pingResultRepository.findTop50ByMonitorIdOrderByTimestampDesc(monitorId);
    }

    public List<AlertLog> getUserAlertLogs(Long userId) {
        return alertLogRepository.findByUserIdOrderBySentAtDesc(userId);
    }
}
