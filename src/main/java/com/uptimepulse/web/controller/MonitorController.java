package com.uptimepulse.web.controller;

import com.uptimepulse.application.service.MonitorService;
import com.uptimepulse.domain.model.AlertLog;
import com.uptimepulse.domain.model.Monitor;
import com.uptimepulse.domain.model.PingResult;
import com.uptimepulse.domain.model.User;
import com.uptimepulse.infrastructure.persistence.UserRepository;
import com.uptimepulse.web.dto.MonitorRequest;
import com.uptimepulse.web.dto.MonitorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/monitors")
@Tag(name = "Monitors", description = "Website health monitor management, alert logs, and manual ping endpoints")
@SecurityRequirement(name = "bearerAuth")
public class MonitorController {

    private final MonitorService monitorService;
    private final UserRepository userRepository;

    public MonitorController(MonitorService monitorService, UserRepository userRepository) {
        this.monitorService = monitorService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Get all website monitors owned by the authenticated user")
    public ResponseEntity<List<MonitorResponse>> getUserMonitors(@AuthenticationPrincipal Object principal) {
        User user = resolveUser(principal);
        List<MonitorResponse> response = monitorService.getMonitorsByUserId(user.getId()).stream()
                .map(this::toMonitorResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new website health & SSL monitor")
    public ResponseEntity<MonitorResponse> createMonitor(@AuthenticationPrincipal Object principal,
                                                         @RequestBody MonitorRequest request) {
        User user = resolveUser(principal);
        Monitor monitor = monitorService.createMonitor(
                user.getId(),
                request.getName(),
                request.getUrl(),
                request.getMonitorType(),
                request.getTags(),
                request.getInterval()
        );
        return ResponseEntity.ok(toMonitorResponse(monitor));
    }

    @PostMapping("/{id}/ping")
    @Operation(summary = "Trigger a manual live health and SSL check for a monitor")
    public ResponseEntity<PingResult> triggerPing(@AuthenticationPrincipal Object principal, @PathVariable Long id) {
        User user = resolveUser(principal);
        Monitor monitor = monitorService.getMonitorById(id)
                .orElseThrow(() -> new RuntimeException("Monitor not found: " + id));
        ensureOwner(user, monitor);
        return ResponseEntity.ok(monitorService.triggerManualPing(id));
    }

    @PostMapping("/scan")
    @Operation(summary = "Run a one-time scan for a URL without saving a monitor")
    public ResponseEntity<PingResult> scanUrl(@RequestBody MonitorRequest request) {
        PingResult result = monitorService.quickScan(request.getUrl());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get recent historical latency and ping logs for a monitor (top 10)")
    public ResponseEntity<List<PingResult>> getPingResults(@AuthenticationPrincipal Object principal, @PathVariable Long id) {
        User user = resolveUser(principal);
        Monitor monitor = monitorService.getMonitorById(id)
                .orElseThrow(() -> new RuntimeException("Monitor not found: " + id));
        ensureOwner(user, monitor);
        return ResponseEntity.ok(monitorService.getRecentPingResults(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get top 50 historical ping results for sparkline charts")
    public ResponseEntity<List<PingResult>> getPingHistory(@AuthenticationPrincipal Object principal, @PathVariable Long id) {
        User user = resolveUser(principal);
        Monitor monitor = monitorService.getMonitorById(id)
                .orElseThrow(() -> new RuntimeException("Monitor not found: " + id));
        ensureOwner(user, monitor);
        return ResponseEntity.ok(monitorService.getHistoricalPingResults(id, 50));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get recent alert notification history for the authenticated user")
    public ResponseEntity<List<AlertLog>> getUserAlertLogs(@AuthenticationPrincipal Object principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(monitorService.getUserAlertLogs(user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a website monitor by ID")
    public ResponseEntity<Map<String, String>> deleteMonitor(@AuthenticationPrincipal Object principal, @PathVariable Long id) {
        User user = resolveUser(principal);
        Monitor monitor = monitorService.getMonitorById(id)
                .orElseThrow(() -> new RuntimeException("Monitor not found: " + id));
        ensureOwner(user, monitor);
        monitorService.deleteMonitor(id);
        return ResponseEntity.ok(Map.of("message", "Monitor deleted successfully"));
    }

    private void ensureOwner(User user, Monitor monitor) {
        if (!monitor.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You do not have permission to access this monitor");
        }
    }

    private User resolveUser(Object principal) {
        if (principal instanceof User domainUser) {
            return domainUser;
        }
        if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
            return userRepository.findByEmail(springUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + springUser.getUsername()));
        }
        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
        }
        throw new IllegalArgumentException("User unauthenticated");
    }

    private MonitorResponse toMonitorResponse(Monitor monitor) {
        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getMonitorType(),
                monitor.getTags(),
                monitor.getStatus(),
                monitor.getLastLatencyMs(),
                monitor.getSslDaysRemaining(),
                monitor.getCheckIntervalMinutes(),
                monitor.getPublicId()
        );
    }
}
