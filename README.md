# Web Application Health, Response Latency & SSL Certificate Expiry Monitoring System

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4.1-green.svg)
![Security](https://img.shields.io/badge/Security-JWT%20%7C%20BCrypt-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

An enterprise-grade, high-concurrency synthetic health and SSL certificate monitoring system built with **Java 21 Virtual Threads (Project Loom)** and **Spring Boot 3.4**. The platform executes non-blocking HTTP/HTTPS availability checks and raw TCP port probes, calculates time-series response latency percentiles, and dispatches real-time multi-channel incident notifications.

---

## 🌟 Key Features

- **Non-Blocking Synthetic Probes:** Executes HTTP/HTTPS health checks and raw TCP socket probes (Redis `6379`, Postgres `5432`, DNS `53`).
- **SSL Expiry Tracking:** Inspects SSL/TLS certificates and calculates remaining validity days.
- **Multi-Channel Alert Dispatcher:** Sends real-time alerts via **Slack (Block Kit)**, **Discord (Embeds)**, **Custom HTTP JSON Webhooks**, and **HTML Emails** (`JavaMailSender`).
- **Persistent Time-Series Metrics:** Embedded file-based H2 database storage capturing historical ping logs and rendering client-side SVG sparklines.
- **Stateless JWT Security & SSRF Defense:** Authenticates users via JWT tokens, BCrypt password hashing, and enforces strict URL sanitation against internal loopback probing.
- **Modern Glassmorphic UI:** Features tag search filtering (`#production`, `#api`), instant domain analyzer scans, and 90-day public status pages.

---

## 🏗️ Technology Stack

- **Core Runtime:** Java 21 (Virtual Threads / Project Loom)
- **Framework:** Spring Boot 3.4, Spring Security, Spring Data JPA
- **Database:** Persistent File-Based H2 Database (`./data/uptimepulse_db`)
- **Messaging & Alerting:** Slack Incoming Webhooks, Discord Embeds, HTTP POST Webhooks, SMTP Java Mail
- **Frontend:** Glassmorphic Dark UI (Vanilla JavaScript ES6+, HTML5, CSS3, SVG Sparklines)
- **Containerization:** Docker & Docker Compose
- **API Documentation:** OpenAPI 3.0 / Swagger UI

---

## 🚀 Quick Start

### 1. Run Locally with Maven
```bash
mvn clean compile spring-boot:run
```
Access the application at `http://localhost:8080`

### 2. Run with Docker Compose
```bash
docker-compose up -d --build
```

---

## 📖 API Documentation & Swagger UI

Interactive Swagger documentation is available at:
`http://localhost:8080/swagger-ui/index.html`

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
