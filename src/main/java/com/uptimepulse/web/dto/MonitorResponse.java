package com.uptimepulse.web.dto;

import com.uptimepulse.domain.enums.MonitorStatus;

public class MonitorResponse {

    private Long id;
    private String name;
    private String url;
    private String monitorType;
    private String tags;
    private MonitorStatus status;
    private Long lastLatencyMs;
    private Integer sslDaysRemaining;
    private Integer checkIntervalMinutes;
    private String publicId;

    public MonitorResponse() {
    }

    public MonitorResponse(Long id, String name, String url, String monitorType, String tags, MonitorStatus status, Long lastLatencyMs, Integer sslDaysRemaining, Integer checkIntervalMinutes, String publicId) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.monitorType = monitorType;
        this.tags = tags;
        this.status = status;
        this.lastLatencyMs = lastLatencyMs;
        this.sslDaysRemaining = sslDaysRemaining;
        this.checkIntervalMinutes = checkIntervalMinutes;
        this.publicId = publicId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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
}
