/* ============================================================
   UPTIMEPULSE — Core Application & Dashboard Client
   Strict Authentication State, Zero Auto-Login,
   Dynamic Landing / Dashboard Views & Connected APIs
   ============================================================ */

const API_BASE = '/api/v1';
let token = localStorage.getItem('up_token') || '';
let userEmail = localStorage.getItem('up_email') || '';
let userFullName = localStorage.getItem('up_fullname') || '';
let cachedMonitors = [];
let currentFilter = 'ALL';
let currentSearch = '';

/* ─── Initialization ─── */
document.addEventListener('DOMContentLoaded', () => {
  initAuthState();

  // Auto-refresh monitors only when authenticated
  setInterval(() => {
    if (token) {
      loadMonitors();
      loadAlerts();
    }
  }, 30000);
});

/* ─── Auth State Management ─── */
function initAuthState() {
  if (token) {
    // Verify token with backend
    apiCall('/auth/me', 'GET')
      .then(user => {
        userEmail = user.email || userEmail;
        userFullName = user.fullName || userEmail.split('@')[0] || 'User';
        setAppState(true);
        loadDashboardData();
      })
      .catch(() => {
        // Invalid or expired token: clear and show public landing
        clearAuthState();
        setAppState(false);
      });
  } else {
    // Unauthenticated: show clean public landing page
    setAppState(false);
  }
}

function setAppState(isLoggedIn) {
  const loggedInNav = document.getElementById('loggedInNav');
  const loggedOutNav = document.getElementById('loggedOutNav');
  const landingView = document.getElementById('landingView');
  const dashboardView = document.getElementById('dashboardView');
  const nameDisplay = document.getElementById('userNameDisplay');
  const initial = document.getElementById('userInitial');

  if (isLoggedIn) {
    if (loggedInNav) loggedInNav.style.display = 'flex';
    if (loggedOutNav) loggedOutNav.style.display = 'none';
    if (landingView) landingView.style.display = 'none';
    if (dashboardView) dashboardView.style.display = 'block';
    if (nameDisplay) nameDisplay.textContent = userFullName || userEmail;
    if (initial) initial.textContent = (userFullName || userEmail || 'U')[0].toUpperCase();
  } else {
    if (loggedInNav) loggedInNav.style.display = 'none';
    if (loggedOutNav) loggedOutNav.style.display = 'flex';
    if (landingView) landingView.style.display = 'block';
    if (dashboardView) dashboardView.style.display = 'none';
  }
}

function clearAuthState() {
  token = '';
  userEmail = '';
  userFullName = '';
  localStorage.removeItem('up_token');
  localStorage.removeItem('up_email');
  localStorage.removeItem('up_fullname');
}

function handleLogout() {
  clearAuthState();
  setAppState(false);
  cachedMonitors = [];
  renderMonitors([]);
  renderAlertFeed([]);
  toast('Signed out successfully.', 'info');
}

function saveAuthToken(data) {
  token = data.token;
  userEmail = data.email || '';
  userFullName = data.fullName || userEmail.split('@')[0] || 'User';
  localStorage.setItem('up_token', token);
  localStorage.setItem('up_email', userEmail);
  localStorage.setItem('up_fullname', userFullName);
}

/* ─── Generic API Helper ─── */
function apiCall(endpoint, method = 'GET', body = null, requireAuth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (requireAuth && token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const options = { method, headers };
  if (body) {
    options.body = JSON.stringify(body);
  }

  return fetch(API_BASE + endpoint, options)
    .then(res => {
      if (res.status === 401 || res.status === 403) {
        if (requireAuth) {
          clearAuthState();
          setAppState(false);
        }
        throw new Error('Unauthorized');
      }
      if (!res.ok) {
        return res.json().then(err => { throw new Error(err.message || 'Request failed'); });
      }
      return res.json();
    });
}

/* ─── Dashboard Data Loading ─── */
function loadDashboardData() {
  loadMonitors();
  loadAlerts();
}

function refreshMonitors() {
  const btn = document.getElementById('refreshBtn');
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span>';
  }

  Promise.all([loadMonitors(), loadAlerts()])
    .finally(() => {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-rotate"></i> Refresh';
      }
      toast('Targets refreshed with live probe telemetry.', 'success');
    });
}

function loadMonitors() {
  return apiCall('/monitors')
    .then(list => {
      cachedMonitors = Array.isArray(list) ? list : [];
      updateKpis(cachedMonitors);
      filterAndRender();
      // Load historical sparkline curves for each monitor
      cachedMonitors.forEach(m => fetchSparkline(m.id, m.status));
      return cachedMonitors;
    })
    .catch(() => {});
}

/* ─── Search & Status Filtering ─── */
function applyStatusFilter(status, btn) {
  currentFilter = status;
  document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
  if (btn) btn.classList.add('active');
  filterAndRender();
}

function handleSearchFilter(query) {
  currentSearch = (query || '').toLowerCase().trim();
  filterAndRender();
}

function filterAndRender() {
  let list = cachedMonitors.slice();

  if (currentFilter === 'UP') {
    list = list.filter(m => m.status === 'UP');
  } else if (currentFilter === 'ISSUES') {
    list = list.filter(m => m.status !== 'UP');
  }

  if (currentSearch) {
    list = list.filter(m =>
      (m.name || '').toLowerCase().includes(currentSearch) ||
      (m.url || '').toLowerCase().includes(currentSearch) ||
      (m.tags || '').toLowerCase().includes(currentSearch)
    );
  }

  renderMonitors(list);
}

/* ─── Render Monitors Grid ─── */
function renderMonitors(monitors) {
  const container = document.getElementById('monitorsContainer');
  if (!container) return;
  container.innerHTML = '';

  if (!monitors || monitors.length === 0) {
    container.innerHTML = `
      <div class="card" style="grid-column: 1 / -1; text-align: center; padding: 3rem 1.5rem; color: var(--text-muted);">
        <i class="fa-solid fa-satellite-dish" style="font-size: 2.25rem; color: var(--primary); margin-bottom: 0.75rem; display: block;"></i>
        <h4 style="color: var(--text-title); margin-bottom: 0.25rem;">No Monitored Targets Found</h4>
        <p style="font-size: 0.85rem; margin-bottom: 1.25rem;">Add your web services or API endpoints to begin synthetic health tracking.</p>
        <button class="btn btn-primary btn-sm" onclick="openAddMonitorModal()">
          <i class="fa-solid fa-plus"></i> Add First Monitor
        </button>
      </div>`;
    return;
  }

  monitors.forEach(m => {
    const isUp = m.status === 'UP';
    const isDeg = m.status === 'DEGRADED';
    const badgeCls = isUp ? 'badge-up' : (isDeg ? 'badge-degraded' : 'badge-down');
    const pulseCls = isUp ? 'pulse-green' : (isDeg ? 'pulse-amber' : 'pulse-red');
    const statusText = m.status || 'PENDING';
    const latText = m.lastLatencyMs != null ? `${m.lastLatencyMs} ms` : '— ms';
    const latColor = isUp ? 'text-emerald' : (isDeg ? 'text-amber' : 'text-rose');
    const sslText = m.sslDaysRemaining != null ? `${m.sslDaysRemaining}d` : 'N/A';
    const sslColor = !m.sslDaysRemaining ? 'var(--text-muted)' : m.sslDaysRemaining > 30 ? 'var(--success)' : 'var(--warning)';
    const typeBadge = m.monitorType === 'TCP' ? '<span style="font-size:0.65rem;background:var(--primary-light);color:var(--primary);padding:0.1rem 0.4rem;border-radius:4px;font-weight:700;">TCP</span>' : '';
    const tagBadges = (m.tags || 'production').split(',').map(t => `<span style="font-size:0.65rem;color:var(--text-muted);background:var(--bg-card-muted);border:1px solid var(--border);padding:0.1rem 0.35rem;border-radius:4px;">#${t.trim()}</span>`).join(' ');

    container.insertAdjacentHTML('beforeend', `
      <div class="card monitor-box" id="monitor-${m.id}">
        <div>
          <div class="monitor-box-head">
            <div style="overflow: hidden;">
              <div style="display: flex; align-items: center; gap: 0.35rem;">
                ${typeBadge}
                <span class="monitor-title" title="${m.name}">${m.name}</span>
              </div>
              <a href="${m.url}" target="_blank" rel="noopener" class="monitor-link" title="${m.url}">${m.url}</a>
              <div style="margin-top: 0.35rem; display: flex; gap: 0.3rem; flex-wrap: wrap;">${tagBadges}</div>
            </div>
            <span class="badge ${badgeCls}">
              <span class="pulse-dot ${pulseCls}"></span> ${statusText}
            </span>
          </div>

          <div class="monitor-mini-stats">
            <div>
              <div class="mini-stat-lbl">Latency</div>
              <div class="mini-stat-val ${latColor}">${latText}</div>
            </div>
            <div>
              <div class="mini-stat-lbl">Interval</div>
              <div class="mini-stat-val">${m.checkIntervalMinutes || 3}m</div>
            </div>
            <div>
              <div class="mini-stat-lbl">SSL Valid</div>
              <div class="mini-stat-val" style="color:${sslColor}">${sslText}</div>
            </div>
          </div>

          <div class="sparkline-box">
            <div class="sparkline-head">
              <span>Live Response Curve</span>
              <span id="sparkline-val-${m.id}">${latText}</span>
            </div>
            <div id="sparkline-${m.id}">
              <svg class="sparkline-svg" viewBox="0 0 300 28" preserveAspectRatio="none">
                <line x1="0" y1="14" x2="300" y2="14" stroke="rgba(15,23,42,0.08)" stroke-dasharray="4"/>
              </svg>
            </div>
          </div>
        </div>

        <div class="monitor-box-foot">
          <button class="btn btn-outline btn-sm" onclick="openAnalyticsModal(${m.id}, '${m.name.replace(/'/g, "\\'")}')">
            <i class="fa-solid fa-chart-line"></i> Analytics
          </button>

          <div class="monitor-btns">
            <a href="/status.html?id=${m.publicId || ''}" target="_blank" class="btn btn-outline btn-sm btn-icon" title="View Public Status">
              <i class="fa-solid fa-arrow-up-right-from-square"></i>
            </a>
            <button class="btn btn-outline btn-sm btn-icon" title="Ping Now" onclick="triggerManualPing(${m.id})">
              <i class="fa-solid fa-rotate"></i>
            </button>
            <button class="btn btn-danger btn-sm btn-icon" title="Delete Monitor" onclick="deleteTarget(${m.id}, '${m.name.replace(/'/g, "\\'")}')">
              <i class="fa-solid fa-trash-can"></i>
            </button>
          </div>
        </div>
      </div>
    `);
  });
}

/* ─── Real DB Sparkline Generation ─── */
function fetchSparkline(monitorId, status) {
  apiCall(`/monitors/${monitorId}/history`, 'GET')
    .then(history => {
      const container = document.getElementById(`sparkline-${monitorId}`);
      if (!container || !Array.isArray(history) || history.length === 0) return;

      const points = history.slice().reverse().map(h => h.latencyMs || 10);
      const isUp = status === 'UP';
      const strokeColor = isUp ? '#059669' : '#e11d48';

      const min = Math.min(...points), max = Math.max(...points, min + 1);
      const W = 300, H = 28, pad = 3;
      const svgPoints = points.map((v, i) => {
        const x = pad + (points.length > 1 ? (i * (W - 2 * pad) / (points.length - 1)) : W / 2);
        const y = H - pad - ((v - min) / (max - min)) * (H - 2 * pad);
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      }).join(' ');

      container.innerHTML = `
        <svg class="sparkline-svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none">
          <polyline fill="none" stroke="${strokeColor}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" points="${svgPoints}"/>
        </svg>`;
    })
    .catch(() => {});
}

/* ─── KPI Calculations ─── */
function updateKpis(monitors) {
  const total = monitors.length;
  const up = monitors.filter(m => m.status === 'UP').length;
  const uptimePct = total > 0 ? ((up / total) * 100).toFixed(2) + '%' : '100%';
  const avgLat = total > 0
    ? Math.round(monitors.reduce((acc, m) => acc + (m.lastLatencyMs || 0), 0) / total) + ' ms'
    : '0 ms';
  const validSsl = monitors.filter(m => (m.sslDaysRemaining || 0) > 30).length;
  const sslPct = total > 0 ? Math.round((validSsl / total) * 100) + '% Valid' : '100% Valid';

  document.getElementById('kpiUptime').textContent = uptimePct;
  document.getElementById('kpiMonitors').textContent = total;
  document.getElementById('kpiLatency').textContent = avgLat;
  document.getElementById('kpiSSL').textContent = sslPct;

  document.getElementById('kpiUptimeMeta').textContent = total ? `${up} of ${total} targets online` : 'All targets healthy';
  document.getElementById('kpiMonitorsMeta').textContent = total ? 'Continuous monitoring active' : 'Synthetic probing active';
  document.getElementById('kpiSSLMeta').textContent = total ? `${validSsl} certs > 30 days remaining` : 'All certificates valid';
}

/* ─── Manual Ping ─── */
function triggerManualPing(id) {
  toast('Sending immediate synthetic probe...', 'info');
  apiCall(`/monitors/${id}/ping`, 'POST')
    .then(result => {
      const isUp = result.status === 'UP';
      toast(`Probe Response: ${result.status} (${result.latencyMs} ms)`, isUp ? 'success' : 'error');
      loadMonitors();
    })
    .catch(err => {
      toast('Ping failed: ' + err.message, 'error');
    });
}

/* ─── Delete Monitor ─── */
function deleteTarget(id, name) {
  if (!confirm(`Delete monitor "${name}" and all historical ping data?`)) return;
  apiCall(`/monitors/${id}`, 'DELETE')
    .then(() => {
      toast('Monitor deleted successfully.', 'success');
      loadMonitors();
    })
    .catch(err => toast('Delete failed: ' + err.message, 'error'));
}

/* ─── Create Monitor ─── */
function openAddMonitorModal() {
  if (!token) {
    window.location.href = '/login.html';
    return;
  }
  document.getElementById('addMonitorModal').style.display = 'flex';
}

function closeAddMonitorModal() {
  document.getElementById('addMonitorModal').style.display = 'none';
}

function handleCreateMonitor(e) {
  e.preventDefault();
  const btn = document.getElementById('addMonitorBtn');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Creating...';

  const body = {
    name: document.getElementById('monName').value.trim(),
    url: document.getElementById('monUrl').value.trim(),
    monitorType: document.getElementById('monType').value,
    interval: parseInt(document.getElementById('monInterval').value, 10),
    tags: document.getElementById('monTags').value.trim() || 'production'
  };

  apiCall('/monitors', 'POST', body)
    .then(() => {
      toast('Target monitor added and initial probe initiated!', 'success');
      closeAddMonitorModal();
      document.getElementById('addMonitorForm').reset();
      loadMonitors();
    })
    .catch(err => toast('Failed to add monitor: ' + err.message, 'error'))
    .finally(() => {
      btn.disabled = false;
      btn.innerHTML = '<i class="fa-solid fa-plus"></i> Create Target';
    });
}

/* ─── Instant Scanner Bar (Logged-In Tool) ─── */
function runInstantScan() {
  if (!token) {
    toast('Please sign in to scan targets and check SSL certificates.', 'info');
    setTimeout(() => { window.location.href = '/login.html'; }, 1000);
    return;
  }

  const url = (document.getElementById('scannerUrl').value || '').trim();
  if (!url) {
    toast('Please enter a target URL or host:port.', 'error');
    return;
  }

  const resultsArea = document.getElementById('scannerResults');
  resultsArea.style.display = 'grid';
  document.getElementById('scanHost').textContent = url;
  document.getElementById('scanLatency').textContent = 'Scanning...';
  document.getElementById('scanSSL').textContent = 'Probing...';
  document.getElementById('scanStatusBadge').innerHTML = '<span class="spinner"></span>';

  apiCall('/monitors/scan', 'POST', { url: url }, false)
    .then(data => {
      const status = data.status || 'UP';
      const latVal = data.latencyMs != null ? data.latencyMs : data.lastLatencyMs;
      const isUp = status === 'UP';
      const badgeCls = isUp ? 'badge-up' : 'badge-down';
      const pulseCls = isUp ? 'pulse-green' : 'pulse-red';
      const sslDays = data.sslDaysRemaining != null && data.sslDaysRemaining > 0 ? `${data.sslDaysRemaining} Days` : 'N/A';

      document.getElementById('scanStatusBadge').innerHTML = `
        <span class="badge ${badgeCls}"><span class="pulse-dot ${pulseCls}"></span> ${status}</span>`;
      document.getElementById('scanLatency').textContent = latVal != null ? `${latVal} ms` : '—';
      document.getElementById('scanSSL').textContent = sslDays;
      toast('Instant probe completed successfully.', 'success');
    })
    .catch(err => {
      document.getElementById('scanStatusBadge').innerHTML = '<span class="badge badge-down">FAILED</span>';
      document.getElementById('scanLatency').textContent = 'Error';
      document.getElementById('scanSSL').textContent = 'Error';
      toast('Scan error: ' + err.message, 'error');
    });
}

/* ─── Analytics Deep-Dive Modal ─── */
function openAnalyticsModal(id, name) {
  document.getElementById('analyticsModal').style.display = 'flex';
  document.getElementById('analyticsTitle').innerHTML = `<i class="fa-solid fa-chart-line" style="color:var(--primary)"></i> ${name} — Performance Analytics`;

  const tbody = document.getElementById('analyticsLogsBody');
  tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:1.5rem;color:var(--text-muted);"><span class="spinner"></span> Loading historical logs...</td></tr>';

  apiCall(`/monitors/${id}/results`)
    .then(results => {
      if (!results || results.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:1.5rem;color:var(--text-muted);">No recorded checks yet. Initial probe running.</td></tr>';
        document.getElementById('anaMinLat').textContent = '—';
        document.getElementById('anaAvgLat').textContent = '—';
        document.getElementById('anaMaxLat').textContent = '—';
        return;
      }

      const lats = results.map(r => r.latencyMs || 0).filter(l => l > 0);
      const min = lats.length ? Math.min(...lats) + ' ms' : '—';
      const max = lats.length ? Math.max(...lats) + ' ms' : '—';
      const avg = lats.length ? Math.round(lats.reduce((a, b) => a + b, 0) / lats.length) + ' ms' : '—';

      document.getElementById('anaMinLat').textContent = min;
      document.getElementById('anaAvgLat').textContent = avg;
      document.getElementById('anaMaxLat').textContent = max;

      tbody.innerHTML = results.map(r => {
        const timeStr = r.timestamp ? new Date(r.timestamp).toLocaleTimeString() : 'Recent';
        const isUp = r.status === 'UP';
        const statusColor = isUp ? 'var(--success)' : 'var(--danger)';
        return `
          <tr style="border-bottom: 1px solid var(--border);">
            <td style="padding: 0.5rem 0.75rem; color: var(--text-muted);">${timeStr}</td>
            <td style="padding: 0.5rem 0.75rem; font-weight: 700; color: ${statusColor};">${r.status}</td>
            <td style="padding: 0.5rem 0.75rem;">${r.statusCode || 200}</td>
            <td style="padding: 0.5rem 0.75rem;">${r.latencyMs || 0} ms</td>
            <td style="padding: 0.5rem 0.75rem;">${r.sslDaysRemaining != null ? r.sslDaysRemaining + 'd' : 'N/A'}</td>
          </tr>
        `;
      }).join('');
    })
    .catch(() => {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:1.5rem;color:var(--danger);">Failed to retrieve historical telemetry.</td></tr>';
    });
}

function closeAnalyticsModal() {
  document.getElementById('analyticsModal').style.display = 'none';
}

/* ─── Alert Feed ─── */
function loadAlerts() {
  return apiCall('/monitors/alerts')
    .then(alerts => renderAlertFeed(Array.isArray(alerts) ? alerts : []))
    .catch(() => renderAlertFeed([]));
}

function renderAlertFeed(alerts) {
  const feed = document.getElementById('alertFeedContainer');
  if (!feed) return;

  if (!alerts || alerts.length === 0) {
    feed.innerHTML = `
      <div class="empty-feed">
        <i class="fa-solid fa-shield-check"></i>
        <strong style="display:block;color:var(--text-title);margin-bottom:0.2rem">All Systems Operational</strong>
        Zero downtime or SSL expiration alerts detected.
      </div>`;
    return;
  }

  feed.innerHTML = '';
  alerts.slice(0, 15).forEach(a => {
    const timeStr = a.sentAt
      ? new Date(a.sentAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
      : 'Recently';
    const chan = a.channelType && a.channelType !== 'LOG_ONLY' ? ` &bull; ${a.channelType}` : '';

    feed.insertAdjacentHTML('beforeend', `
      <div class="alert-card">
        <div class="alert-card-head">
          <span class="alert-tag"><i class="fa-solid fa-triangle-exclamation"></i> INCIDENT${chan}</span>
          <span class="alert-time">${timeStr}</span>
        </div>
        <div class="alert-text">${a.alertMessage || 'Downtime detected on probe target.'}</div>
      </div>
    `);
  });
}

/* ─── Webhook Notification Channels ─── */
function openNotificationsModal() {
  document.getElementById('notificationsModal').style.display = 'flex';
  loadWebhooks();
}

function closeNotificationsModal() {
  document.getElementById('notificationsModal').style.display = 'none';
}

function loadWebhooks() {
  const container = document.getElementById('webhooksList');
  container.innerHTML = '<div style="text-align:center;padding:1rem;color:var(--text-muted);"><span class="spinner"></span> Loading channels...</div>';

  apiCall('/notifications')
    .then(channels => {
      if (!channels || channels.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:1rem;color:var(--text-muted);font-size:0.85rem;">No webhook channels connected. Add Slack or Discord below.</div>';
        return;
      }

      container.innerHTML = channels.map(c => `
        <div style="display:flex;align-items:center;justify-content:space-between;padding:0.6rem 0.85rem;background:var(--bg-card-muted);border:1px solid var(--border);border-radius:var(--radius-md);margin-bottom:0.5rem;">
          <div>
            <strong style="font-size:0.85rem;color:var(--text-title);display:block;">${c.channelName}</strong>
            <span style="font-size:0.75rem;color:var(--text-muted);">${c.channelType} &bull; ${c.destination}</span>
          </div>
          <button class="btn btn-danger btn-sm" onclick="deleteWebhookChannel(${c.id})"><i class="fa-solid fa-trash-can"></i></button>
        </div>
      `).join('');
    })
    .catch(() => {
      container.innerHTML = '<div style="text-align:center;padding:1rem;color:var(--danger);font-size:0.85rem;">Failed to load notification webhooks.</div>';
    });
}

function handleCreateWebhook(e) {
  e.preventDefault();
  const btn = document.getElementById('saveWebhookBtn');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Testing &amp; Saving...';

  const body = {
    name: document.getElementById('webhookName').value.trim(),
    channelType: document.getElementById('webhookType').value,
    webhookUrl: document.getElementById('webhookDestination').value.trim()
  };

  apiCall('/notifications', 'POST', body)
    .then(() => {
      toast('Alert channel saved and verification test alert sent!', 'success');
      document.getElementById('addWebhookForm').reset();
      loadWebhooks();
    })
    .catch(err => toast('Error saving webhook: ' + err.message, 'error'))
    .finally(() => {
      btn.disabled = false;
      btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Save &amp; Send Test Alert';
    });
}

function deleteWebhookChannel(id) {
  if (!confirm('Remove this notification webhook?')) return;
  apiCall(`/notifications/${id}`, 'DELETE')
    .then(() => {
      toast('Notification webhook removed.', 'success');
      loadWebhooks();
    })
    .catch(err => toast('Failed to remove: ' + err.message, 'error'));
}

/* ─── Toast System ─── */
function toast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const icons = {
    success: { icon: 'fa-circle-check', color: 'var(--success)' },
    error:   { icon: 'fa-circle-xmark', color: 'var(--danger)' },
    info:    { icon: 'fa-circle-info',  color: 'var(--primary)' }
  };
  const { icon, color } = icons[type] || icons.info;

  const el = document.createElement('div');
  el.className = 'toast';
  el.innerHTML = `<i class="fa-solid ${icon}" style="color:${color};flex-shrink:0;"></i><span>${message}</span>`;
  container.appendChild(el);

  setTimeout(() => {
    el.style.opacity = '0';
    el.style.transform = 'translateX(110%)';
    setTimeout(() => el.remove(), 250);
  }, 3200);
}
