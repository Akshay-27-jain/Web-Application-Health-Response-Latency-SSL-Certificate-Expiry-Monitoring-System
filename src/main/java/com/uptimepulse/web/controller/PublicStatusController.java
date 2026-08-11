package com.uptimepulse.web.controller;

import com.uptimepulse.application.service.MonitorService;
import com.uptimepulse.domain.model.Monitor;
import com.uptimepulse.domain.model.PingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public Status", description = "Unauthenticated public status pages for shared monitors")
public class PublicStatusController {

    private final MonitorService monitorService;

    public PublicStatusController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/status/{publicId}")
    @Operation(summary = "Get public status, latency, and SSL uptime history for a shared monitor")
    public ResponseEntity<Map<String, Object>> getPublicStatus(@PathVariable String publicId) {
        Monitor monitor = monitorService.getMonitorByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Public status page not found for ID: " + publicId));

        List<PingResult> recentHistory = monitorService.getRecentPingResults(monitor.getId());
        long totalChecks = recentHistory.size();
        long upCount = recentHistory.stream().filter(r -> r.getStatus().name().equals("UP")).count();
        String uptime = totalChecks == 0 ? "100%" : String.format("%.2f%%", (upCount * 100.0) / totalChecks);

        Map<String, Object> response = new HashMap<>();
        response.put("publicId", monitor.getPublicId());
        response.put("name", monitor.getName());
        response.put("url", monitor.getUrl());
        response.put("status", monitor.getStatus());
        response.put("lastLatencyMs", monitor.getLastLatencyMs());
        response.put("sslDaysRemaining", monitor.getSslDaysRemaining());
        response.put("uptimePercentage", uptime);
        response.put("recentHistory", recentHistory);
        return ResponseEntity.ok(response);
    }
}
