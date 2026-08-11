package com.uptimepulse.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class SslCheckerService {

    private static final Logger log = LoggerFactory.getLogger(SslCheckerService.class);

    public int getSslDaysRemaining(String urlString) {
        if (urlString == null || !urlString.toLowerCase().startsWith("https://")) {
            return 0;
        }
        HttpsURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            Certificate[] certs = conn.getServerCertificates();
            if (certs != null) {
                for (Certificate certificate : certs) {
                    if (certificate instanceof X509Certificate x509) {
                        Date expiresOn = x509.getNotAfter();
                        long diffInMillis = expiresOn.getTime() - System.currentTimeMillis();
                        long days = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
                        return Math.max(0, (int) days);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("SSL Check failed for URL {}: {}", urlString, e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }
}
