package com.uptimepulse.application.service;

import com.uptimepulse.domain.enums.MonitorStatus;
import com.uptimepulse.domain.model.PingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HealthPingService {

    private static final Logger log = LoggerFactory.getLogger(HealthPingService.class);
    private final SslCheckerService sslCheckerService;
    private final HttpClient httpClient;

    public HealthPingService(SslCheckerService sslCheckerService) {
        this.sslCheckerService = sslCheckerService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static String normalizeUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.isBlank()) {
            throw new IllegalArgumentException("Monitor URL/Host must not be empty");
        }
        String trimmed = inputUrl.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.contains(":")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    public PingResult pingUrl(Long monitorId, String rawUrl) {
        return pingUrl(monitorId, rawUrl, "HTTP");
    }

    public PingResult pingUrl(Long monitorId, String rawUrl, String type) {
        if ("TCP".equalsIgnoreCase(type)) {
            return pingTcpPort(monitorId, rawUrl);
        }

        String urlString = normalizeUrl(rawUrl);

        // SSRF Guard
        if (!isSafePublicUrl(urlString)) {
            return new PingResult(monitorId, MonitorStatus.DOWN, 400, 0L, 0, "SSRF Guard: Private or loopback IP range blocked");
        }

        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("User-Agent", "UptimePulse-HealthMonitor/2.0")
                    .GET()
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long latency = System.currentTimeMillis() - start;

            int code = response.statusCode();
            MonitorStatus status = (code >= 200 && code < 400) ? MonitorStatus.UP : MonitorStatus.DEGRADED;
            int sslDays = sslCheckerService.getSslDaysRemaining(urlString);

            return new PingResult(monitorId, status, code, latency, sslDays, null);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("Health ping failed for {}: {}", urlString, e.getMessage());
            return new PingResult(monitorId, MonitorStatus.DOWN, 500, latency, 0, e.getMessage());
        }
    }

    private PingResult pingTcpPort(Long monitorId, String hostPort) {
        long start = System.currentTimeMillis();
        try {
            String host = hostPort;
            int port = 80;
            if (hostPort.contains(":")) {
                String[] parts = hostPort.split(":");
                host = parts[0].replace("http://", "").replace("https://", "");
                port = Integer.parseInt(parts[1]);
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
            }
            long latency = System.currentTimeMillis() - start;
            return new PingResult(monitorId, MonitorStatus.UP, 200, latency, 0, null, "TCP Probe");
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new PingResult(monitorId, MonitorStatus.DOWN, 500, latency, 0, "TCP Connection failed: " + e.getMessage(), "TCP Probe");
        }
    }

    private boolean isSafePublicUrl(String urlString) {
        try {
            URI uri = URI.create(urlString);
            String host = uri.getHost();
            if (host == null) return false;

            String lower = host.toLowerCase();
            if (lower.equals("localhost") || lower.endsWith(".local") || lower.equals("0.0.0.0")) {
                return false;
            }

            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
