package com.uptimepulse.web.controller;

import com.uptimepulse.infrastructure.persistence.MonitorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Probe server location and system metadata")
public class SystemInfoController {

    private final MonitorRepository monitorRepository;
    private final long startTime = System.currentTimeMillis();

    public SystemInfoController(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    @GetMapping("/info")
    @Operation(summary = "Get probe server location and system metadata")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        String hostname = "Local Probe Node";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}

        long uptimeSec = (System.currentTimeMillis() - startTime) / 1000;

        return ResponseEntity.ok(Map.of(
                "probeNode", hostname,
                "probeLocation", "Local Datacenter / Self-Hosted",
                "javaVersion", System.getProperty("java.version", "21"),
                "activeMonitors", monitorRepository.count(),
                "serverUptimeSeconds", uptimeSec,
                "status", "HEALTHY"
        ));
    }
}
