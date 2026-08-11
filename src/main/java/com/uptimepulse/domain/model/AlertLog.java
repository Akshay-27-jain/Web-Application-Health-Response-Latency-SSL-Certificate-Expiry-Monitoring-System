package com.uptimepulse.domain.model;

import com.uptimepulse.domain.enums.AlertChannelType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_logs")
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_id", nullable = false)
    private Long monitorId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "alert_message", nullable = false, length = 1024)
    private String alertMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private AlertChannelType channelType;

    private Boolean success = true;

    @Column(name = "sent_at")
    private LocalDateTime sentAt = LocalDateTime.now();

    public AlertLog() {}

    public AlertLog(Long monitorId, Long userId, String alertMessage, AlertChannelType channelType, Boolean success) {
        this.monitorId = monitorId;
        this.userId = userId;
        this.alertMessage = alertMessage;
        this.channelType = channelType;
        this.success = success;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMonitorId() { return monitorId; }
    public void setMonitorId(Long monitorId) { this.monitorId = monitorId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }

    public AlertChannelType getChannelType() { return channelType; }
    public void setChannelType(AlertChannelType channelType) { this.channelType = channelType; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
