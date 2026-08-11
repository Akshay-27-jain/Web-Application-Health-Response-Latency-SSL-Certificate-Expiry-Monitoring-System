package com.uptimepulse.domain.model;

import com.uptimepulse.domain.enums.MonitorStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ping_results")
public class PingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_id", nullable = false)
    private Long monitorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorStatus status;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "ssl_days_remaining")
    private Integer sslDaysRemaining;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "check_from")
    private String checkFrom = "Local Server";

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public PingResult() {
    }

    public PingResult(Long monitorId, MonitorStatus status, Integer statusCode, Long latencyMs, Integer sslDaysRemaining, String errorMessage) {
        this.monitorId = monitorId;
        this.status = status;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.sslDaysRemaining = sslDaysRemaining;
        this.errorMessage = errorMessage;
        this.checkFrom = "Local Probe";
    }

    public PingResult(Long monitorId, MonitorStatus status, Integer statusCode, Long latencyMs, Integer sslDaysRemaining, String errorMessage, String checkFrom) {
        this(monitorId, status, statusCode, latencyMs, sslDaysRemaining, errorMessage);
        this.checkFrom = checkFrom;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(Long monitorId) {
        this.monitorId = monitorId;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public void setStatus(MonitorStatus status) {
        this.status = status;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Integer getSslDaysRemaining() {
        return sslDaysRemaining;
    }

    public void setSslDaysRemaining(Integer sslDaysRemaining) {
        this.sslDaysRemaining = sslDaysRemaining;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCheckFrom() {
        return checkFrom;
    }

    public void setCheckFrom(String checkFrom) {
        this.checkFrom = checkFrom;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
