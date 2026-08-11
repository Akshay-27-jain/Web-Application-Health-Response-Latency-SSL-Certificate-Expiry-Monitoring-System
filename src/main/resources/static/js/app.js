/* ═══════════════════════════════════════
   UptimePulse — Dashboard Application Logic
   Auto-login, API calls, monitors grid,
   alert feed, real DB sparklines, webhooks, analytics modal, search & filter
═══════════════════════════════════════ */

const API = '/api/v1';
let authToken = localStorage.getItem('up_token') || '';
let userEmail  = localStorage.getItem('up_email')  || '';
let monitorsCache = [];
let activeStatusFilter = 'ALL';
let searchQuery = '';

/* ─── Bootstrap ─── */
document.addEventListener('DOMContentLoaded', () => {
  updateAuthUi();
  loadSystemInfo();
  if (authToken) {
    loadAll();
  } else {
    autoLogin();
  }
  // Auto-refresh every 30s
  setInterval(() => { if (authToken) loadAll(); }, 30_000);
});

/* ─── System Probe Info ─── */
function loadSystemInfo() {
  fetch(API + '/system/info')
    .then(r => r.json())
    .then(d => {
      const txt = document.getElementById('probeNodeText');
      if (txt) txt.textContent = `Probe: ${d.probeNode || 'Local Server'}`;
    })
    .catch(() => {});
}

/* ─── Auth UI ─── */
function updateAuthUi() {
  const badge  = document.getElementById('userBadge');
  const btn    = document.getElementById('loginNavBtn');
  const email  = document.getElementById('userEmailDisplay');
  const init   = document.getElementById('userInitial');
  if (authToken) {
    if (badge) badge.style.display = 'flex';
    if (btn)   btn.style.display = 'none';
    if (email) email.textContent = userEmail;
    if (init)  init.textContent = (userEmail[0] || 'U').toUpperCase();
  } else {
    if (badge) badge.style.display = 'none';
    if (btn)   btn.style.display = 'inline-flex';
  }
}

function autoLogin() {
  return apiFetch('/auth/login', 'POST', { email: 'user@uptimepulse.com', password: 'password123' }, false)
    .then(data => {
      if (data && data.token) storeToken(data);
      loadAll();
      return data;
    })
    .catch(() => loadAll());
}

function storeToken(data) {
  authToken = data.token;
  userEmail  = data.email || '';
  localStorage.setItem('up_token', authToken);
  localStorage.setItem('up_email', userEmail);
  updateAuthUi();
}

function handleLogout() {
  authToken = '';
  userEmail  = '';
  localStorage.removeItem('up_token');
  localStorage.removeItem('up_email');
  updateAuthUi();
  monitorsCache = [];
  renderMonitors([]);
  renderAlertFeed([]);
  updateStats([]);
  toast('Logged out successfully', 'info');
  setTimeout(() => openAuthModal('login'), 400);
}

/* ─── Load Everything ─── */
function loadAll() {
  loadMonitors();
  loadAlerts();
}

function refreshAll() {
  const btn = document.getElementById('refreshBtn');
  if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner"></span>'; }
  Promise.all([loadMonitors(), loadAlerts()]).finally(() => {
    if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-rotate"></i> Refresh'; }
    toast('Dashboard refreshed!', 'success');
  });
}

/* ─── Generic API fetch ─── */
function apiFetch(path, method = 'GET', body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && authToken) headers['Authorization'] = `Bearer ${authToken}`;
  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);
  return fetch(API + path, opts)
    .then(res => {
      if (res.status === 401 || res.status === 403) {
        // Clear stale token & auto-relogin
        localStorage.removeItem('up_token');
        authToken = '';
        updateAuthUi();
        autoLogin();
        throw new Error('Session refreshed. Please submit again.');
      }
      if (!res.ok) return res.json().then(e => { throw new Error(e.message || 'Request failed'); });
      return res.json();
    });
}

/* ─── Search & Filter Controls ─── */
function filterByStatus(status, btn) {
  activeStatusFilter = status;
  document.querySelectorAll('.filter-pill').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  applyFilters();
}

function handleSearch(query) {
  searchQuery = (query || '').toLowerCase().trim();
  applyFilters();
}

function applyFilters() {
  let filtered = monitorsCache.slice();

  if (activeStatusFilter === 'UP') {
    filtered = filtered.filter(m => m.status === 'UP');
  } else if (activeStatusFilter === 'ISSUES') {
    filtered = filtered.filter(m => m.status !== 'UP');
  }

  if (searchQuery) {
    filtered = filtered.filter(m =>
      (m.name || '').toLowerCase().includes(searchQuery) ||
      (m.url || '').toLowerCase().includes(searchQuery) ||
      (m.tags || '').toLowerCase().includes(searchQuery)
    );
  }

  renderMonitors(filtered);
}

/* ─── Monitors ─── */
function loadMonitors() {
  return apiFetch('/monitors')
    .then(data => {
      const list = Array.isArray(data) ? data : [];
      monitorsCache = list;
      applyFilters();
      updateStats(list);
      list.forEach(m => loadMonitorHistory(m.id, m.status));
      return list;
    })
    .catch(err => { console.warn('[monitors]', err); renderMonitors([]); updateStats([]); });
}

function renderMonitors(monitors) {
  const grid = document.getElementById('monitorsGrid');
  if (!grid) return;
  grid.innerHTML = '';

  if (!monitors || monitors.length === 0) {
    grid.innerHTML = `
      <div class="empty-state card">
        <i class="fa-solid fa-satellite-dish"></i>
        <p>No website monitors match your filter.<br>Add one to start tracking uptime.</p>
        <button class="btn btn-primary" onclick="openAddMonitorModal()">
          <i class="fa-solid fa-plus"></i> Add First Monitor
        </button>
      </div>`;
    return;
  }

  monitors.forEach(m => {
    const isUp  = m.status === 'UP';
    const isDeg = m.status === 'DEGRADED';
    const badgeClass   = isUp ? 'badge-up' : (isDeg ? 'badge-degraded' : 'badge-down');
    const pulseClass   = isUp ? 'pulse-green' : (isDeg ? 'pulse-amber' : 'pulse-red');
    const statusLabel  = m.status || 'PENDING';
    const latencyColor = isUp ? 'text-emerald' : (isDeg ? 'text-amber' : 'text-rose');
    const sslColor = !m.sslDaysRemaining ? 'text-muted' : m.sslDaysRemaining > 30 ? 'text-emerald' : m.sslDaysRemaining > 7 ? 'text-amber' : 'text-rose';
    const sslVal   = m.sslDaysRemaining != null ? `${m.sslDaysRemaining}d` : 'N/A';
    const latVal   = m.lastLatencyMs != null ? `${m.lastLatencyMs} ms` : '— ms';
    const typeTag  = m.monitorType === 'TCP' ? '<span style="font-size:0.65rem;background:rgba(99,102,241,0.15);color:var(--accent-blue);padding:0.1rem 0.4rem;border-radius:4px;font-weight:700">TCP</span>' : '';
    const tagBadges = (m.tags || 'production').split(',').map(t => `<span style="font-size:0.65rem;color:var(--text-secondary);background:rgba(255,255,255,0.05);padding:0.1rem 0.35rem;border-radius:4px">#${t.trim()}</span>`).join(' ');

    grid.insertAdjacentHTML('beforeend', `
      <div class="card monitor-card fade-in" id="monitor-card-${m.id}">
        <div class="monitor-top">
          <div style="overflow:hidden">
            <div style="display:flex;align-items:center;gap:0.35rem">
              ${typeTag}
              <div class="monitor-name" title="${m.name}">${m.name}</div>
            </div>
            <a href="${m.url}" target="_blank" rel="noopener" class="monitor-url" title="${m.url}">${m.url}</a>
            <div style="margin-top:0.25rem">${tagBadges}</div>
          </div>
          <span class="badge ${badgeClass}">
            <span class="pulse-dot ${pulseClass}"></span> ${statusLabel}
          </span>
        </div>

        <div class="monitor-stats-row">
          <div class="monitor-stat">
            <div class="monitor-stat-label">Latency</div>
            <div class="monitor-stat-val ${latencyColor}">${latVal}</div>
          </div>
          <div class="monitor-stat">
            <div class="monitor-stat-label">Interval</div>
            <div class="monitor-stat-val">${m.checkIntervalMinutes}m</div>
          </div>
          <div class="monitor-stat">
            <div class="monitor-stat-label">SSL Exp.</div>
            <div class="monitor-stat-val ${sslColor}">${sslVal}</div>
          </div>
        </div>

        <div class="sparkline-wrap">
          <div class="sparkline-label"><span>Live Ping History</span><span id="sparkline-val-${m.id}">${latVal}</span></div>
          <div id="sparkline-container-${m.id}">
            <svg class="sparkline-svg" viewBox="0 0 300 30" preserveAspectRatio="none">
              <line x1="0" y1="15" x2="300" y2="15" stroke="rgba(255,255,255,0.1)" stroke-dasharray="4"/>
            </svg>
          </div>
        </div>

        <div class="monitor-footer">
          <button class="btn btn-outline btn-sm" onclick="openAnalyticsModal(${m.id}, '${m.name.replace(/'/g, "\\'")}')">
            <i class="fa-solid fa-chart-line"></i> Analytics
          </button>
          <div class="monitor-actions">
            <a href="/status.html?id=${m.publicId || ''}" target="_blank" class="btn btn-outline btn-sm btn-icon" title="Public Status">
              <i class="fa-solid fa-arrow-up-right-from-square"></i>
            </a>
            <button class="btn btn-outline btn-sm btn-icon" title="Ping Now" onclick="pingMonitor(${m.id})">
              <i class="fa-solid fa-rotate"></i>
            </button>
            <button class="btn btn-danger btn-sm btn-icon" title="Delete" onclick="deleteMonitor(${m.id}, '${m.name.replace(/'/g, "\\'")}')">
              <i class="fa-solid fa-trash-can"></i>
            </button>
          </div>
        </div>
      </div>
    `);
  });
}

/* ─── Real DB Sparkline Renderer ─── */
function loadMonitorHistory(monitorId, status) {
  apiFetch(`/monitors/${monitorId}/history`)
    .then(history => {
      const container = document.getElementById(`sparkline-container-${monitorId}`);
      if (!container || !Array.isArray(history) || history.length === 0) return;

      const pts = history.slice().reverse().map(h => h.latencyMs || 10);
      const isUp = status === 'UP';
      const isDeg = status === 'DEGRADED';
      const strokeColor = isUp ? '#10b981' : (isDeg ? '#f59e0b' : '#f43f5e');

      const min = Math.min(...pts), max = Math.max(...pts, min + 1);
      const W = 300, H = 30, pad = 4;
      const svgPts = pts.map((v, i) => {
        const x = pad + (pts.length > 1 ? (i * (W - 2 * pad) / (pts.length - 1)) : W / 2);
        const y = H - pad - ((v - min) / (max - min)) * (H - 2 * pad);
        return `${x},${y}`;
      }).join(' ');

      container.innerHTML = `
        <svg class="sparkline-svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none">
          <polyline fill="none" stroke="${strokeColor}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" points="${svgPts}"/>
        </svg>`;
    })
    .catch(() => {});
}

/* ─── Stats Metrics ─── */
function updateStats(monitors) {
  const total  = monitors.length;
  const up     = monitors.filter(m => m.status === 'UP').length;
  const uptimePct = total ? ((up / total) * 100).toFixed(1) + '%' : '100%';
  const avgLat = total
    ? Math.round(monitors.reduce((s, m) => s + (m.lastLatencyMs || 0), 0) / total) + ' ms'
    : '0 ms';
  const goodSSL = monitors.filter(m => (m.sslDaysRemaining || 0) > 30).length;
  const sslPct  = total ? Math.round((goodSSL / total) * 100) + '% Valid' : '100% Valid';

  setText('statUptime',  uptimePct);
  setText('statMonitors', total);
  setText('statLatency',  avgLat);
  setText('statSSL',      sslPct);

  const uptimeMeta = total ? `${up} of ${total} sites online` : 'All monitors operational';
  setText('statUptimeMeta',   uptimeMeta);
  setText('statMonitorsMeta', total ? 'Health & SSL checks running' : 'Active monitoring enabled');
  setText('statSSLMeta',      total ? `${goodSSL} certs > 30 days valid` : 'All certificates valid');
}

/* ─── Alerts ─── */
function loadAlerts() {
  return apiFetch('/monitors/alerts')
    .then(data => renderAlertFeed(Array.isArray(data) ? data : []))
    .catch(() => renderAlertFeed([]));
}

function renderAlertFeed(alerts) {
  const feed  = document.getElementById('alertFeed');
  const badge = document.getElementById('alertCountBadge');
  if (!feed) return;

  if (!alerts || alerts.length === 0) {
    feed.innerHTML = `
      <div class="no-alerts">
        <i class="fa-solid fa-shield-check"></i>
        <strong style="display:block;margin-bottom:0.3rem">All Clear!</strong>
        No downtime or SSL alerts detected.
      </div>`;
    if (badge) badge.style.display = 'none';
    return;
  }

  if (badge) { badge.textContent = alerts.length; badge.style.display = 'inline'; }

  feed.innerHTML = '';
  alerts.slice(0, 20).forEach(a => {
    const timeStr = a.sentAt
      ? new Date(a.sentAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
      : 'Recently';
    const channelTag = a.channelType && a.channelType !== 'LOG_ONLY' ? ` [${a.channelType}]` : '';
    feed.insertAdjacentHTML('beforeend', `
      <div class="alert-item">
        <div class="alert-item-header">
          <span class="alert-item-tag"><i class="fa-solid fa-triangle-exclamation"></i> ALERT${channelTag}</span>
          <span class="alert-item-time">${timeStr}</span>
        </div>
        <div class="alert-item-msg">${a.alertMessage || 'Downtime detected'}</div>
      </div>
    `);
  });
}

/* ─── Analytics Modal ─── */
function openAnalyticsModal(id, name) {
  document.getElementById('analyticsModal').style.display = 'flex';
  document.getElementById('analyticsTitle').innerHTML = `<i class="fa-solid fa-chart-line" style="color:var(--accent-blue)"></i> ${name} — Performance Logs`;

  const tbody = document.getElementById('analyticsLogsTable');
  tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:1rem;color:var(--text-secondary)"><span class="spinner"></span> Loading logs...</td></tr>';

  apiFetch(`/monitors/${id}/results`)
    .then(results => {
      if (!results || results.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:1rem;color:var(--text-secondary)">No health checks recorded yet.</td></tr>';
        setText('analyticsMinLat', '—');
        setText('analyticsAvgLat', '—');
        setText('analyticsMaxLat', '—');
        return;
      }

      const lats = results.map(r => r.latencyMs || 0).filter(l => l > 0);
      const min = lats.length ? Math.min(...lats) + ' ms' : '—';
      const max = lats.length ? Math.max(...lats) + ' ms' : '—';
      const avg = lats.length ? Math.round(lats.reduce((a,b)=>a+b,0)/lats.length) + ' ms' : '—';

      setText('analyticsMinLat', min);
      setText('analyticsAvgLat', avg);
      setText('analyticsMaxLat', max);

      tbody.innerHTML = results.map(r => {
        const timeStr = r.timestamp ? new Date(r.timestamp).toLocaleTimeString() : 'Recently';
        const isUp = r.status === 'UP';
        const statusColor = isUp ? 'var(--accent-emerald)' : 'var(--accent-rose)';
        return `
          <tr style="border-bottom:1px solid var(--border-color)">
            <td style="padding:0.6rem 0.8rem;color:var(--text-secondary)">${timeStr}</td>
            <td style="padding:0.6rem 0.8rem;font-weight:700;color:${statusColor}">${r.status}</td>
            <td style="padding:0.6rem 0.8rem">${r.statusCode || 200}</td>
            <td style="padding:0.6rem 0.8rem">${r.latencyMs || 0} ms</td>
            <td style="padding:0.6rem 0.8rem">${r.sslDaysRemaining != null ? r.sslDaysRemaining + 'd' : 'N/A'}</td>
          </tr>
        `;
      }).join('');
    })
    .catch(() => {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:1rem;color:var(--accent-rose)">Failed to load results.</td></tr>';
    });
}

function closeAnalyticsModal() {
  document.getElementById('analyticsModal').style.display = 'none';
}

/* ─── Ping Monitor ─── */
function pingMonitor(id) {
  toast('Pinging…', 'info');
  apiFetch(`/monitors/${id}/ping`, 'POST')
    .then(data => {
      toast(`Ping: ${data.status} — ${data.latencyMs} ms`, data.status === 'UP' ? 'success' : 'error');
      loadMonitors();
    })
    .catch(err => toast('Ping failed: ' + err.message, 'error'));
}

/* ─── Delete Monitor ─── */
function deleteMonitor(id, name) {
  if (!confirm(`Delete monitor "${name}"?`)) return;
  apiFetch(`/monitors/${id}`, 'DELETE')
    .then(() => { toast('Monitor deleted', 'success'); loadMonitors(); })
    .catch(err => toast('Delete failed: ' + err.message, 'error'));
}

/* ─── Add Monitor ─── */
function handleAddMonitor(e) {
  e.preventDefault();
  const btn = document.getElementById('addMonitorSubmitBtn');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Creating…';

  const sendCreate = () => {
    apiFetch('/monitors', 'POST', {
      name:        document.getElementById('monName').value,
      url:         document.getElementById('monUrl').value,
      monitorType: document.getElementById('monType').value,
      tags:        document.getElementById('monTags').value,
      interval:    parseInt(document.getElementById('monInterval').value)
    })
    .then(() => {
      toast('Monitor created!', 'success');
      closeAddMonitorModal();
      document.getElementById('addMonitorForm').reset();
      loadMonitors();
    })
    .catch(err => toast('Failed: ' + err.message, 'error'))
    .finally(() => {
      btn.disabled = false;
      btn.innerHTML = '<i class="fa-solid fa-plus"></i> Create Monitor';
    });
  };

  if (!authToken) {
    autoLogin().then(sendCreate).catch(sendCreate);
  } else {
    sendCreate();
  }
}

/* ─── Webhook Notifications Channel Management ─── */
function openNotificationsModal() {
  if (!authToken) {
    autoLogin().then(() => {
      document.getElementById('notificationsModal').style.display = 'flex';
      loadNotificationChannels();
    });
    return;
  }
  document.getElementById('notificationsModal').style.display = 'flex';
  loadNotificationChannels();
}
function closeNotificationsModal() {
  document.getElementById('notificationsModal').style.display = 'none';
}

function loadNotificationChannels() {
  const container = document.getElementById('notificationsList');
  if (!container) return;

  apiFetch('/notifications')
    .then(configs => {
      if (!configs || configs.length === 0) {
        container.innerHTML = `<p style="font-size:0.85rem;color:var(--text-secondary);text-align:center;padding:1rem">No webhooks or Slack/Discord channels configured yet.</p>`;
        return;
      }
      container.innerHTML = configs.map(c => `
        <div style="display:flex;justify-content:space-between;align-items:center;padding:0.75rem 1rem;background:rgba(255,255,255,0.03);border:1px solid var(--border-color);border-radius:var(--radius-md);margin-bottom:0.5rem">
          <div>
            <div style="font-size:0.9rem;font-weight:700;color:#fff">${c.name}</div>
            <div style="font-size:0.75rem;color:var(--text-secondary)">Type: <strong style="color:var(--accent-violet)">${c.channelType}</strong> — ${c.webhookUrl}</div>
          </div>
          <button class="btn btn-danger btn-sm btn-icon" title="Delete Channel" onclick="deleteNotificationChannel(${c.id})">
            <i class="fa-solid fa-trash-can"></i>
          </button>
        </div>
      `).join('');
    })
    .catch(() => {
      container.innerHTML = `<p style="font-size:0.85rem;color:var(--accent-rose);text-align:center">Failed to load channels.</p>`;
    });
}

function handleAddNotificationChannel(e) {
  e.preventDefault();
  const btn = document.getElementById('saveWebhookBtn');
  btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Saving…';

  apiFetch('/notifications', 'POST', {
    name:        document.getElementById('webhookName').value,
    channelType: document.getElementById('webhookChannelType').value,
    webhookUrl:  document.getElementById('webhookUrl').value
  })
  .then(() => {
    toast('Notification webhook saved & test alert sent!', 'success');
    document.getElementById('addWebhookForm').reset();
    loadNotificationChannels();
  })
  .catch(err => toast('Failed to save webhook: ' + err.message, 'error'))
  .finally(() => {
    btn.disabled = false;
    btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Save &amp; Test Webhook';
  });
}

function deleteNotificationChannel(id) {
  if (!confirm('Delete this notification channel?')) return;
  apiFetch(`/notifications/${id}`, 'DELETE')
    .then(() => {
      toast('Notification channel removed', 'success');
      loadNotificationChannels();
    })
    .catch(err => toast('Failed to delete channel: ' + err.message, 'error'));
}

/* ─── Instant Analyzer ─── */
function runAnalyzer() {
  const url = (document.getElementById('analyzerUrl').value || '').trim();
  if (!url) { toast('Enter a URL to scan', 'error'); return; }

  const resultBox = document.getElementById('analyzerResult');
  resultBox.style.display = 'block';
  document.getElementById('analyzerUrl2').textContent   = url;
  document.getElementById('analyzerLatency').textContent = 'Scanning…';
  document.getElementById('analyzerSSL').textContent    = 'Checking…';
  document.getElementById('analyzerStatusBadge').innerHTML = '';

  apiFetch('/monitors/scan', 'POST', { url: url }, false)
  .then(data => {
    const status     = data.status || 'UP';
    const latVal     = data.latencyMs != null ? data.latencyMs : data.lastLatencyMs;
    const latency    = latVal != null ? `${latVal} ms` : '—';
    const sslDays    = data.sslDaysRemaining != null && data.sslDaysRemaining > 0 ? `${data.sslDaysRemaining} Days` : 'N/A';
    const isUp       = status === 'UP';
    const badgeCls   = isUp ? 'badge-up' : 'badge-down';
    const pulseCls   = isUp ? 'pulse-green' : 'pulse-red';

    document.getElementById('analyzerStatusBadge').innerHTML = `
      <span class="badge ${badgeCls}">
        <span class="pulse-dot ${pulseCls}"></span> ${status}
      </span>`;
    document.getElementById('analyzerLatency').textContent = latency;
    document.getElementById('analyzerSSL').textContent     = sslDays;
    toast('Scan complete!', 'success');
  })
  .catch(err => {
    document.getElementById('analyzerLatency').textContent = 'Error';
    document.getElementById('analyzerSSL').textContent    = 'Error';
    toast('Scan failed: ' + err.message, 'error');
  });
}

/* ─── Auth Modal ─── */
function openAuthModal(tab = 'login') {
  document.getElementById('authModal').style.display = 'flex';
  switchAuthTab(tab);
}
function closeAuthModal() {
  document.getElementById('authModal').style.display = 'none';
}
function switchAuthTab(tab) {
  const loginForm = document.getElementById('authLoginForm');
  const regForm   = document.getElementById('authRegisterForm');
  const lBtn      = document.getElementById('tabLoginBtn');
  const rBtn      = document.getElementById('tabRegisterBtn');
  if (tab === 'login') {
    loginForm.style.display = 'block';
    regForm.style.display   = 'none';
    lBtn.classList.add('active');
    rBtn.classList.remove('active');
  } else {
    loginForm.style.display = 'none';
    regForm.style.display   = 'block';
    rBtn.classList.add('active');
    lBtn.classList.remove('active');
  }
}
function fillCreds(email, pwd) {
  document.getElementById('loginEmail').value = email;
  document.getElementById('loginPwd').value   = pwd;
}

function handleLoginSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('loginSubmitBtn');
  btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Signing in…';

  apiFetch('/auth/login', 'POST', {
    email:    document.getElementById('loginEmail').value,
    password: document.getElementById('loginPwd').value
  }, false)
  .then(data => {
    storeToken(data);
    closeAuthModal();
    toast(`Welcome back, ${data.fullName || data.email}!`, 'success');
    loadAll();
  })
  .catch(err => toast('Login failed: ' + err.message, 'error'))
  .finally(() => { btn.disabled = false; btn.innerHTML = 'Sign In'; });
}

function handleRegisterSubmit(e) {
  e.preventDefault();
  const btn = document.getElementById('regSubmitBtn');
  btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Creating…';

  apiFetch('/auth/register', 'POST', {
    fullName: document.getElementById('regName').value,
    email:    document.getElementById('regEmail').value,
    password: document.getElementById('regPwd').value
  }, false)
  .then(data => {
    storeToken(data);
    closeAuthModal();
    toast(`Account created! Welcome, ${data.fullName || data.email}!`, 'success');
    loadAll();
  })
  .catch(err => toast('Registration failed: ' + err.message, 'error'))
  .finally(() => { btn.disabled = false; btn.innerHTML = 'Create Account'; });
}

/* ─── Add Monitor Modal ─── */
function openAddMonitorModal() {
  if (!authToken) {
    autoLogin().then(() => {
      document.getElementById('addMonitorModal').style.display = 'flex';
    });
    return;
  }
  document.getElementById('addMonitorModal').style.display = 'flex';
}
function closeAddMonitorModal() {
  document.getElementById('addMonitorModal').style.display = 'none';
}

/* ─── Toast Notifications ─── */
function toast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const colors = {
    success: { icon: 'fa-circle-check',  color: 'var(--accent-emerald)' },
    error:   { icon: 'fa-circle-xmark',  color: 'var(--accent-rose)' },
    info:    { icon: 'fa-circle-info',   color: 'var(--accent-blue)' },
  };
  const { icon, color } = colors[type] || colors.info;

  const t = document.createElement('div');
  t.className = 'toast';
  t.innerHTML = `<i class="fa-solid ${icon}" style="color:${color};flex-shrink:0"></i><span>${message}</span>`;
  container.appendChild(t);

  setTimeout(() => {
    t.style.opacity   = '0';
    t.style.transform = 'translateX(110%)';
    setTimeout(() => t.remove(), 300);
  }, 3000);
}

/* ─── Utility ─── */
function setText(id, val) {
  const el = document.getElementById(id);
  if (el) el.textContent = val;
}

// Close modals on backdrop click
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.style.display = 'none';
  }
});
