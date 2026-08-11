package com.uptimepulse.domain.model;

import com.uptimepulse.domain.enums.AlertChannelType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_configs")
public class NotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private AlertChannelType channelType;

    @Column(name = "webhook_url", nullable = false, length = 1024)
    private String webhookUrl;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public NotificationConfig() {}

    public NotificationConfig(Long userId, String name, AlertChannelType channelType, String webhookUrl) {
        this.userId = userId;
        this.name = name;
        this.channelType = channelType;
        this.webhookUrl = webhookUrl;
        this.enabled = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AlertChannelType getChannelType() { return channelType; }
    public void setChannelType(AlertChannelType channelType) { this.channelType = channelType; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
