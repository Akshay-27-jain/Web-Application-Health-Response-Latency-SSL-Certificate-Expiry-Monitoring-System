package com.uptimepulse.web.controller;

import com.uptimepulse.application.service.MonitorService;
import com.uptimepulse.domain.enums.MonitorStatus;
import com.uptimepulse.domain.model.Monitor;
import com.uptimepulse.domain.model.PingResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public Status", description = "Unauthenticated public status pages for shared monitors")
public class PublicStatusController {

    private final MonitorService monitorService;

    public PublicStatusController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get global public system overview metrics and active services")
    public ResponseEntity<Map<String, Object>> getGlobalOverview() {
        List<Monitor> all = monitorService.getAllMonitors();
        long total = all.size();
        long upCount = all.stream().filter(m -> m.getStatus() == MonitorStatus.UP).count();
        double uptimePct = total > 0 ? (upCount * 100.0) / total : 100.0;
        double avgLatency = total > 0 ? all.stream().mapToLong(m -> m.getLastLatencyMs() != null ? m.getLastLatencyMs() : 0).average().orElse(0) : 0;
        long validSsl = all.stream().filter(m -> m.getSslDaysRemaining() != null && m.getSslDaysRemaining() > 30).count();
        double sslPct = total > 0 ? (validSsl * 100.0) / total : 100.0;

        Map<String, Object> res = new HashMap<>();
        res.put("totalMonitors", total);
        res.put("upMonitors", upCount);
        res.put("uptimePercentage", String.format("%.2f%%", uptimePct));
        res.put("avgLatencyMs", Math.round(avgLatency));
        res.put("sslHealthPercentage", String.format("%.1f%%", sslPct));
        res.put("services", all.stream().limit(8).map(m -> Map.of(
                "name", m.getName(),
                "url", m.getUrl(),
                "status", m.getStatus().name(),
                "latencyMs", m.getLastLatencyMs() != null ? m.getLastLatencyMs() : 0,
                "sslDaysRemaining", m.getSslDaysRemaining() != null ? m.getSslDaysRemaining() : 0,
                "publicId", m.getPublicId() != null ? m.getPublicId() : ""
        )).collect(Collectors.toList()));
        return ResponseEntity.ok(res);
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
