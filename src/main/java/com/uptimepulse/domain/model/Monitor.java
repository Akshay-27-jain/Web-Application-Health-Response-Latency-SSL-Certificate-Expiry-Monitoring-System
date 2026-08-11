package com.uptimepulse.domain.model;

import com.uptimepulse.domain.enums.MonitorStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(name = "monitor_type")
    private String monitorType = "HTTP"; // HTTP or TCP

    @Column(name = "tags")
    private String tags = "production";

    @Column(name = "check_interval_minutes", nullable = false)
    private Integer checkIntervalMinutes = 3;

    @Column(name = "public_id", unique = true, nullable = false)
    private String publicId = UUID.randomUUID().toString().substring(0, 12);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorStatus status = MonitorStatus.UP;

    @Column(name = "last_latency_ms")
    private Long lastLatencyMs = 0L;

    @Column(name = "ssl_days_remaining")
    private Integer sslDaysRemaining = 0;

    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    @Column(name = "last_alert_sent_at")
    private LocalDateTime lastAlertSentAt;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Monitor() {
    }

    public Monitor(Long userId, String name, String url, Integer checkIntervalMinutes) {
        this.userId = userId;
        this.name = name;
        this.url = url;
        this.checkIntervalMinutes = checkIntervalMinutes != null ? checkIntervalMinutes : 3;
        this.publicId = "pub-" + UUID.randomUUID().toString().substring(0, 8);
        this.tags = "production";
        this.monitorType = "HTTP";
    }

    public Monitor(Long userId, String name, String url, String monitorType, String tags, Integer checkIntervalMinutes) {
        this(userId, name, url, checkIntervalMinutes);
        if (monitorType != null && !monitorType.isBlank()) this.monitorType = monitorType.toUpperCase();
        if (tags != null && !tags.isBlank()) this.tags = tags.toLowerCase();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMonitorType() {
        return monitorType != null ? monitorType : "HTTP";
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public String getTags() {
        return tags != null ? tags : "production";
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getCheckIntervalMinutes() {
        return checkIntervalMinutes;
    }

    public void setCheckIntervalMinutes(Integer checkIntervalMinutes) {
        this.checkIntervalMinutes = checkIntervalMinutes;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public void setStatus(MonitorStatus status) {
        this.status = status;
    }

    public Long getLastLatencyMs() {
        return lastLatencyMs;
    }

    public void setLastLatencyMs(Long lastLatencyMs) {
        this.lastLatencyMs = lastLatencyMs;
    }

    public Integer getSslDaysRemaining() {
        return sslDaysRemaining;
    }

    public void setSslDaysRemaining(Integer sslDaysRemaining) {
        this.sslDaysRemaining = sslDaysRemaining;
    }

    public Integer getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(Integer consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public LocalDateTime getLastAlertSentAt() {
        return lastAlertSentAt;
    }

    public void setLastAlertSentAt(LocalDateTime lastAlertSentAt) {
        this.lastAlertSentAt = lastAlertSentAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
