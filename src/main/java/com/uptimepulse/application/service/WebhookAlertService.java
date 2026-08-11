package com.uptimepulse.application.service;

import com.uptimepulse.domain.enums.AlertChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class WebhookAlertService {

    private static final Logger log = LoggerFactory.getLogger(WebhookAlertService.class);
    private final HttpClient httpClient;

    public WebhookAlertService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Async
    public void dispatchAlert(AlertChannelType channelType, String webhookUrl, String monitorName, String monitorUrl, String alertMessage, String status) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        try {
            String payload = buildJsonPayload(channelType, monitorName, monitorUrl, alertMessage, status);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "UptimePulse-AlertDispatcher/2.0")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(6))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(res -> log.info("[ALERT SENT] Channel: {} | Status: {} | Target: {}", channelType, res.statusCode(), webhookUrl))
                    .exceptionally(ex -> {
                        log.warn("[ALERT FAILED] Channel: {} | Error: {}", channelType, ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.warn("Failed to construct alert payload for {}: {}", webhookUrl, e.getMessage());
        }
    }

    private String buildJsonPayload(AlertChannelType channelType, String monitorName, String monitorUrl, String alertMessage, String status) {
        return switch (channelType) {
            case SLACK -> String.format("""
                {
                  "text": "🚨 *UptimePulse Alert: %s*",
                  "attachments": [
                    {
                      "color": "%s",
                      "fields": [
                        { "title": "Monitor", "value": "%s", "short": true },
                        { "title": "Status", "value": "%s", "short": true },
                        { "title": "URL", "value": "%s", "short": false },
                        { "title": "Details", "value": "%s", "short": false }
                      ]
                    }
                  ]
                }
                """, escape(monitorName), "DOWN".equalsIgnoreCase(status) ? "#f43f5e" : "#f59e0b",
                    escape(monitorName), escape(status), escape(monitorUrl), escape(alertMessage));

            case DISCORD -> String.format("""
                {
                  "username": "UptimePulse Alert",
                  "avatar_url": "https://raw.githubusercontent.com/feathericons/feather/master/icons/heart-pulse.svg",
                  "embeds": [
                    {
                      "title": "🚨 Monitor Status Alert: %s",
                      "description": "%s",
                      "color": %d,
                      "fields": [
                        { "name": "URL", "value": "%s", "inline": true },
                        { "name": "Status", "value": "%s", "inline": true }
                      ],
                      "timestamp": "%s"
                    }
                  ]
                }
                """, escape(monitorName), escape(alertMessage),
                    "DOWN".equalsIgnoreCase(status) ? 16007006 : 16097547,
                    escape(monitorUrl), escape(status), java.time.Instant.now().toString());

            default -> String.format("""
                {
                  "event": "MONITOR_ALERT",
                  "monitorName": "%s",
                  "monitorUrl": "%s",
                  "status": "%s",
                  "message": "%s",
                  "timestamp": "%s"
                }
                """, escape(monitorName), escape(monitorUrl), escape(status), escape(alertMessage), java.time.Instant.now().toString());
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
