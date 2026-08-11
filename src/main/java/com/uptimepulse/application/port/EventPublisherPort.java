package com.uptimepulse.application.port;

public interface EventPublisherPort {
    void publishAlertEvent(Long monitorId, String message, String status);
}
