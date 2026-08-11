package com.uptimepulse.infrastructure.messaging;

import com.uptimepulse.application.port.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Primary
public class InMemoryEventAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventAdapter.class);

    @Override
    @Async
    public void publishAlertEvent(Long monitorId, String message, String status) {
        log.info("[STANDALONE EVENT BUS] Emitted alert event -> Monitor ID: {}, Status: {}, Message: {}", 
                monitorId, status, message);
    }
}
