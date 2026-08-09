let trendChart = null;
let tokenData = null;
let tokenUsageInfo = null;
let tokenTrendData = null;
let currentUserId = null;
let currentPeriod = 'day';
let currentPage = 1;
let itemsPerPage = 10;
let filteredRecords = [];
let eventsBound = false;

function getCustomerNavMap() {
    const local = window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost';
    const runtimeConfig = window.__APP_NAV_CONFIG__ || {};
    const workbenchUrl = runtimeConfig.workbenchUrl
        || (local ? `${window.location.protocol}//${window.location.hostname}:15174/` : `${window.location.origin}/workbench/`);
    const favoriteScriptUrl = runtimeConfig.favoriteScriptUrl
        || (local ? `${window.location.protocol}//${window.location.hostname}:15278/` : `${window.location.origin}/favorite-script-library/`);
    const tokenUsageUrl = runtimeConfig.tokenUsageUrl
        || (local ? `${window.location.protocol}//${window.location.hostname}:15280/` : `${window.location.origin}/token-usage/`);
    const normalizedWorkbenchUrl = workbenchUrl.endsWith('/') ? workbenchUrl : `${workbenchUrl}/`;

    return {
        '智能对话': normalizedWorkbenchUrl,
        '对话记录': `${normalizedWorkbenchUrl}?view=history`,
        '个人话术库': favoriteScriptUrl,
        '我的评估': `${normalizedWorkbenchUrl}?view=evaluation`,
        'Token统计': tokenUsageUrl,
        '设置': `${normalizedWorkbenchUrl}?view=setting`
    };
}

// ===================== 【新增：和Sidebar.tsx完全一致的侧边栏状态】 =====================
let expandState = {};
// 初始化读取共享存储 key:sidebar_expand
function initExpandState() {
    try {
        const saved = localStorage.getItem("sidebar_expand");
        if (saved) {
            expandState = JSON.parse(saved);
        } else {
            expandState = { dashboard: true };
        }
    } catch (e) {
        expandState = { dashboard: true };
    }
}
function toggleExpand(parentId) {
    expandState[parentId] = !expandState[parentId];
    localStorage.setItem("sidebar_expand", JSON.stringify(expandState));
    applySidebarRender();
}


// 获取dashboard展开状态
function getDashboardOpen() {
    return expandState.dashboard ?? true;
}
// 渲染侧边栏箭头、Token统计显隐
// 渲染侧边栏箭头、Token统计显隐
function applySidebarRender() {
    const dashboardOpen = getDashboardOpen();
    const dashboardToggle = document.getElementById('dashboardSectionToggle');
    const tokenStatsMenuItem = document.getElementById('tokenStatsMenuItem');
    const chevIcon = dashboardToggle?.querySelector('.chev-icon, .script-fold-arrow');
    const customerNavMap = getCustomerNavMap();

    if (!dashboardToggle || !tokenStatsMenuItem) return;

    // 控制Token统计子菜单显示/隐藏
    tokenStatsMenuItem.style.display = dashboardOpen ? 'block' : 'none';

    // =========【新增】自动激活高亮（当前页面匹配则添加active类）=========
    const tokenUrl = customerNavMap['Token统计'];
    // 判断当前地址是否为Token统计页面
    if (window.location.href.startsWith(tokenUrl)) {
        tokenStatsMenuItem.classList.add('active');
    } else {
        tokenStatsMenuItem.classList.remove('active');
    }

    // 箭头旋转逻辑保持不变
    if (chevIcon) {
        if (dashboardOpen) {
            chevIcon.classList.add('rotate-180');
        } else {
            chevIcon.classList.remove('rotate-180');
        }
    }
}
// =================================================================================

function getCustomerId() {
    return currentUserId
        || new URLSearchParams(window.location.search).get('customerId')
        || localStorage.getItem('customerId')
        || '';
}

async function loadTokenData() {
    const userId = getCustomerId();
    if (!window.tokenApi || !window.tokenApi.getRecords) throw new Error('tokenApi not loaded');
    const page = 1;
    const size = 20;
    const res = await window.tokenApi.getRecords(userId, { page, size });
    // server response expected: { items, page, size, totalElements, totalPages }
    const items = res?.items || res?.data?.items || res?.data || res || [];
    const mapped = Array.isArray(items) ? items.map(normalizeRecord) : [];
    const totalElements = res?.totalElements || res?.data?.totalElements || 0;
    const totalPages = res?.totalPages || res?.data?.totalPages || Math.ceil(totalElements / (res?.size || size));
    tokenData = {
        records: mapped,
        page: res?.page || page,
        size: res?.size || size,
        totalElements: totalElements,
        totalPages: totalPages,
        fromServer: true
    };
    updateDataSourceNote();
    return tokenData;
}

async function loadTokenUsageInfo() {
    try {
        const userId = getCustomerId();
        if (window.tokenApi && window.tokenApi.getTodayStats) {
            const res = await window.tokenApi.getTodayStats(userId);
            const data = res?.data || res || null;
            if (data) {
                // ensure usedToday field for UI
                data.usedToday = data.totalTokens ?? data.total_tokens ?? data.total ?? data.usedToday ?? null;
                tokenUsageInfo = data;
                updateDataSourceNote();
            }
            return data;
        }
        return null;
    } catch (error) {
        return null;
    }
}

async function loadSummary() {
    try {
        const userId = getCustomerId();
        if (window.tokenApi && window.tokenApi.getSummary) {
            const res = await window.tokenApi.getSummary(userId);
            const data = res?.data || res || null;
            if (data) {
                tokenUsageInfo = tokenUsageInfo || {};
                tokenUsageInfo.summary = data;
                updateDataSourceNote();
            }
            return data;
        }
    } catch (e) {
        return null;
    }
}

async function loadTrend() {
    try {
        const userId = getCustomerId();
        if (window.tokenApi && window.tokenApi.getTrend) {
            const res = await window.tokenApi.getTrend(userId, getTrendRequestParams());
            const data = res?.data || res || null;
            if (Array.isArray(data)) {
                tokenTrendData = data.map(normalizeTrendPoint).filter(Boolean);
            } else if (data && Array.isArray(data.items)) {
                tokenTrendData = data.items.map(normalizeTrendPoint).filter(Boolean);
            } else {
                tokenTrendData = null;
            }
            if (tokenTrendData) updateDataSourceNote();
            return tokenTrendData;
        }
    } catch (e) {
        return null;
    }
}

// SSE subscription management
let streamConnection = null;
let streamRetry = 1000;
function startStream() {
    if (!window.tokenApi || !window.tokenApi.connectStream) return;
    const userId = getCustomerId();
    if (streamConnection && typeof streamConnection.close === 'function') {
        try { streamConnection.close(); } catch (e) {}
    }
    streamConnection = null;

    const onEvent = (evt) => {
        if (!evt) return;
        // support different event shapes
        const payload = evt.today || evt.summary || evt.data || evt;
        if (payload) {
            tokenUsageInfo = { ...(tokenUsageInfo || {}), ...payload };
            // Refresh the authoritative four-card overview so week, month and
            // history stay consistent with each newly recorded token event.
            loadSummary().then(renderStats).catch(() => renderStats());
        }
        if (evt.recordAdded || evt.newRecord) {
            Promise.all([loadTokenData(), loadTrend()])
                .then(() => { renderTable(); renderChart(); })
                .catch(()=>{});
        }
    };

    const onError = (err) => {
        console.warn('Stream error', err);
        setTimeout(() => {
            streamRetry = Math.min(streamRetry * 1.8, 30000);
            startStream();
        }, streamRetry);
    };

    window.tokenApi.connectStream(userId, onEvent, onError).then(conn => {
        streamConnection = conn;
        streamRetry = 1000;
    }).catch(err => {
        onError(err);
    });
}

function updateDataSourceNote() {
    const note = document.getElementById('dataSourceNote');
    if (note) {
        // Keep the original subtitle line height and spacing without showing
        // the data-source text.
        note.textContent = '\u00a0';
    }
}

function updateAll() {
    renderStats();
    renderChart();
    renderTable();
}

function parseMockDate(value) {
    if (!value) return null;
    if (value instanceof Date) {
        return Number.isNaN(value.getTime()) ? null : value;
    }
    const text = String(value);
    let date;
    if (text.includes('-')) {
        date = new Date(text.replace(/-/g, '/'));
        return Number.isNaN(date.getTime()) ? null : date;
    }
    const parts = text.split('/').map(Number);
    if (parts.length >= 2) {
        date = new Date(2026, parts[0] - 1, parts[1]);
        return Number.isNaN(date.getTime()) ? null : date;
    }
    date = new Date(text);
    return Number.isNaN(date.getTime()) ? null : date;
}

function normalizeDate(date) {
    if (!date) return null;
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function getLatestRecordDate() {
    const dates = (tokenData?.records || [])
        .map(record => parseMockDate(record.callTime))
        .filter(Boolean)
        .map(normalizeDate);
    if (!dates.length) return normalizeDate(new Date());
    return dates.reduce((latest, date) => date > latest ? date : latest, dates[0]);
}

function getRangeDates() {
    const end = getLatestRecordDate();
    const start = new Date(end);
    start.setDate(end.getDate() - 6);
    return {
        start: normalizeDate(start),
        end: normalizeDate(end)
    };
}

function getFilteredRecords() {
    const { start, end } = getRangeDates();
    return (tokenData?.records || []).filter(record => {
        const recordDate = normalizeDate(parseMockDate(record.callTime));
        if (!recordDate) return false;
        return recordDate >= start && recordDate <= end;
    });
}

// Normalize server record shape to frontend-friendly shape (backward compatible)
function normalizeRecord(r) {
    if (!r) return r;
    const record = {};
    record.id = r.requestId || r.idempotencyKey || r.id || r.request_id || r.requestIdRaw || '';
    record.callTime = formatCallTime(
        r.callTime || r.call_time || r.occurredAt || r.occurred_at || r.createdAt || r.created_at || r.timestamp || ''
    );
    // model and provider
    record.modelName = r.model || r.modelName || r.model_name || '';
    record.provider = r.provider || r.vendor || '';
    // tokens
    record.inputToken = Number((r.inputTokens ?? r.inputToken ?? r.input_tokens ?? r.input) || 0);
    record.outputToken = Number((r.outputTokens ?? r.outputToken ?? r.output_tokens ?? r.output) || 0);
    record.totalToken = Number((r.totalTokens ?? r.totalToken ?? r.total_tokens ?? r.total) || (record.inputToken + record.outputToken));
    record.inputContentLength = Number((r.inputContentLength ?? r.input_content_length ?? r.input_length) || 0);
    record.outputContentLength = Number((r.outputContentLength ?? r.output_content_length ?? r.output_length) || 0);
    record.cachedInputTokens = Number((r.cachedInputTokens ?? r.cachedInputTokens ?? r.cached_input_tokens) || 0);
    const status = (r.status || r.callStatus || '').toString().toUpperCase();
    record.status = status === 'SUCCEEDED' || status === 'SUCCESS' ? 'success'
        : status === 'FAILED' || status === 'CANCELLED' ? 'failed' : 'processing';
    record.sessionId = r.conversationId || r.sessionId || r.conversation_id || '';
    record.userId = r.userId || r.user_id || r.user || '';
    record.prompt = r.input || r.prompt || r.request || '';
    record.response = r.response || r.output || r.result || '';
    record.providerReportedCost = Number((r.providerReportedCost ?? r.provider_reported_cost ?? r.providerReportedCost) || 0);
    record.costCurrency = r.costCurrency || r.cost_currency || r.currency || '';
    return record;
}

function formatCallTime(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);

    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const hh = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    const sec = String(date.getSeconds()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd} ${hh}:${min}:${sec}`;
}

function normalizeTrendPoint(p) {
    if (!p) return null;
    const bucket = p.bucketStart || p.bucket_start || p.date || p.day || p.label || '';
    return {
        date: bucket,
        inputToken: Number((p.inputTokens ?? p.inputToken ?? p.input_tokens) || 0),
        outputToken: Number((p.outputTokens ?? p.outputToken ?? p.output_tokens) || 0),
        totalToken: Number((p.totalTokens ?? p.totalToken ?? p.total_tokens) || (p.inputTokens + p.outputTokens) || 0),
        calls: Number((p.calls ?? p.count) || 0)
    };
}

function getWeekStart(date) {
    const clone = new Date(date);
    const day = clone.getDay();
    const diff = (day + 6) % 7;
    clone.setDate(clone.getDate() - diff);
    return normalizeDate(clone);
}

function getDashboardStats() {
    const overview = tokenUsageInfo?.summary;
    const total = (summary) => Number(
        summary?.totalTokens
        ?? summary?.totalToken
        ?? summary?.total_tokens
        ?? 0
    );

    if (overview) {
        return {
            today: total(overview.today),
            week: total(overview.thisWeek ?? overview.week),
            month: total(overview.thisMonth ?? overview.month),
            history: total(overview.history)
        };
    }

    // Keep the independently returned real today value available if the
    // overview request is temporarily unavailable. Never derive card totals
    // from the paginated records list.
    return {
        today: Number(tokenUsageInfo?.usedToday ?? tokenUsageInfo?.totalTokens ?? 0),
        week: 0,
        month: 0,
        history: 0
    };
}

async function initApp() {
    try {
        initExpandState(); // 初始化侧边栏状态
        await loadCurrentUser();
        // Load today/summary/trend/records from backend when available
        tokenUsageInfo = await loadTokenUsageInfo();
        await loadSummary();
        await loadTrend();
        tokenData = await loadTokenData();
        renderStats();
        renderChart();
        renderTable();
        bindEvents();
        applySidebarRender(); // 初次渲染侧边栏状态
        // Start SSE after the initial real-data request succeeds.
        if (tokenData?.fromServer) {
            startStream();
        }
    } catch (error) {
        showErrorState();
        console.error('Failed to initialize app:', error);
    }
}

async function loadCurrentUser() {
    if (!window.tokenApi?.getCurrentUser) throw new Error('tokenApi not loaded');
    const payload = await window.tokenApi.getCurrentUser();
    const user = payload?.data || payload;
    currentUserId = String(user?.userId ?? user?.id ?? '');
    if (!currentUserId) throw new Error('Current user id is missing');
    const displayName = user?.username || user?.account || '客服';
    const nameElement = document.querySelector('.user-name');
    const avatarElement = document.querySelector('.avatar');
    if (nameElement) nameElement.textContent = displayName;
    if (avatarElement) avatarElement.textContent = displayName.trim().slice(0, 1) || '客';
    return user;
}

function getTrendRequestParams() {
    const { start, end } = getTrendRangeDates();

    // The backend treats `to` as the exclusive upper bound. Use the beginning
    // of the following day so the selected end date is included completely.
    const endExclusive = new Date(end);
    endExclusive.setDate(endExclusive.getDate() + 1);

    return {
        range: 'LAST_7_DAYS',
        from: start.toISOString(),
        to: endExclusive.toISOString(),
        bucket: 'DAY'
    };
}

function getTrendRangeDates() {
    let start;
    const end = normalizeDate(new Date());

    // With the date controls removed, each view uses one stable real-data
    // window: 7 days, 12 natural weeks, or 12 natural months.
    if (currentPeriod === 'week') {
        start = getWeekStart(end);
        start.setDate(start.getDate() - 11 * 7);
    } else if (currentPeriod === 'month') {
        start = new Date(end.getFullYear(), end.getMonth() - 11, 1);
    } else {
        start = new Date(end);
        start.setDate(end.getDate() - 6);
    }

    return { start, end };
}

function renderStats() {
    const stats = getDashboardStats();
    const formatNumber = (num) => num.toLocaleString('zh-CN');
    const createTrendText = (trend) => {
        if (trend === 0) return '-';
        const prefix = trend > 0 ? '+' : '';
        const className = trend > 0 ? 'up' : 'down';
        return `<div class="stat-trend ${className}">较上期 ${prefix}${trend}%</div>`;
    };
    const statsCards = document.querySelector('.stats-cards');
    statsCards.innerHTML = `
        <div class="stat-card">
            <div class="stat-icon daily">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M13 2L3 14h7l-1 8L21 10h-7l-1-8z" fill="#2f65ff" />
                </svg>
            </div>
            <div class="stat-info">
                <div class="stat-label">今日Token消耗</div>
                <div class="stat-value">${formatNumber(stats.today)}</div>
            </div>
            ${createTrendText(0)}
        </div>
        <div class="stat-card">
            <div class="stat-icon weekly">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect x="3" y="5" width="18" height="14" rx="2" stroke="#2f65ff" stroke-width="1.4" fill="#f4fbff" />
                  <path d="M3 9h18" stroke="#2f65ff" stroke-width="1.2" />
                  <rect x="7" y="2" width="2" height="4" rx="1" fill="#2f65ff" />
                  <rect x="15" y="2" width="2" height="4" rx="1" fill="#2f65ff" />
                </svg>
            </div>
            <div class="stat-info">
                <div class="stat-label">本周累计消耗</div>
                <div class="stat-value">${formatNumber(stats.week)}</div>
            </div>
            ${createTrendText(0)}
        </div>
        <div class="stat-card">
            <div class="stat-icon monthly">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="12" r="9" stroke="#2f65ff" stroke-width="1.2" fill="#f7fbff" />
                  <path d="M12 7v5l4 2" stroke="#2f65ff" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
            </div>
            <div class="stat-info">
                <div class="stat-label">本月累计消耗</div>
                <div class="stat-value">${formatNumber(stats.month)}</div>
            </div>
            ${createTrendText(0)}
        </div>
        <div class="stat-card">
            <div class="stat-icon history">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <ellipse cx="12" cy="6" rx="8" ry="2.6" stroke="#2f65ff" stroke-width="1.2" fill="#f4fbff" />
                  <path d="M4 6v6c0 1.4 3.6 2.6 8 2.6s8-1.2 8-2.6V6" stroke="#2f65ff" stroke-width="1.2" fill="none" />
                  <path d="M4 12v4c0 1.4 3.6 2.6 8 2.6s8-1.2 8-2.6v-4" stroke="#cfe6ff" stroke-width="1" fill="none" />
                </svg>
            </div>
            <div class="stat-info">
                <div class="stat-label">历史总消耗</div>
                <div class="stat-value">${formatNumber(stats.history)}</div>
            </div>
            <div class="stat-trend">-</div>
        </div>
    `;
}

function buildDailyTrend(records) {
    const { start, end } = getRangeDates();
    const timeline = [];
    const dateMap = {};
    for (let date = new Date(start); date <= end; date.setDate(date.getDate() + 1)) {
        const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
        const label = `${date.getMonth() + 1}/${date.getDate()}`;
        timeline.push(key);
        dateMap[key] = { date: label, inputToken: 0, outputToken: 0, totalToken: 0 };
    }
    records.forEach(record => {
        const recordDate = normalizeDate(parseMockDate(record.callTime));
        const key = `${recordDate.getFullYear()}-${String(recordDate.getMonth() + 1).padStart(2, '0')}-${String(recordDate.getDate()).padStart(2, '0')}`;
        if (!dateMap[key]) return;
        const value = record.totalToken || (record.inputToken + record.outputToken);
        dateMap[key].inputToken += record.inputToken;
        dateMap[key].outputToken += record.outputToken;
        dateMap[key].totalToken += value;
    });
    return timeline.map(key => dateMap[key]);
}

function buildTrendByPeriod(records, period) {
    const groups = {};
    records.forEach(record => {
        const recordDate = normalizeDate(parseMockDate(record.callTime));
        if (!recordDate) return;
        let key;
        let label;
        if (period === 'week') {
            const weekStart = getWeekStart(recordDate);
            const weekEnd = new Date(weekStart);
            weekEnd.setDate(weekStart.getDate() + 6);
            key = `${weekStart.getFullYear()}-${String(weekStart.getMonth() + 1).padStart(2, '0')}-${String(weekStart.getDate()).padStart(2, '0')}`;
            label = `${weekStart.getMonth() + 1}/${weekStart.getDate()}-${weekEnd.getMonth() + 1}/${weekEnd.getDate()}`;
        } else {
            key = `${recordDate.getFullYear()}-${String(recordDate.getMonth() + 1).padStart(2, '0')}`;
            label = `${recordDate.getMonth() + 1}月`;
        }
        if (!groups[key]) {
            groups[key] = { date: label, inputToken: 0, outputToken: 0, totalToken: 0, key };
        }
        const value = record.totalToken || (record.inputToken + record.outputToken);
        groups[key].inputToken += record.inputToken;
        groups[key].outputToken += record.outputToken;
        groups[key].totalToken += value;
    });
    return Object.values(groups).sort((a, b) => a.key.localeCompare(b.key));
}

function buildServerTrendByPeriod(points, period) {
    const groups = {};

    // Seed every natural week/month in the selected trend window. Periods
    // without API events remain zero and are still visible on the timeline.
    const { start, end } = getTrendRangeDates();
    if (period === 'week') {
        const lastWeek = getWeekStart(end);
        for (let cursor = getWeekStart(start); cursor <= lastWeek; cursor.setDate(cursor.getDate() + 7)) {
            const weekStart = new Date(cursor);
            const weekEnd = new Date(weekStart);
            weekEnd.setDate(weekStart.getDate() + 6);
            const key = `${weekStart.getFullYear()}-${String(weekStart.getMonth() + 1).padStart(2, '0')}-${String(weekStart.getDate()).padStart(2, '0')}`;
            groups[key] = {
                date: `${weekStart.getMonth() + 1}/${weekStart.getDate()}-${weekEnd.getMonth() + 1}/${weekEnd.getDate()}`,
                inputToken: 0,
                outputToken: 0,
                totalToken: 0,
                key
            };
        }
    } else {
        const lastMonth = new Date(end.getFullYear(), end.getMonth(), 1);
        for (let cursor = new Date(start.getFullYear(), start.getMonth(), 1); cursor <= lastMonth; cursor.setMonth(cursor.getMonth() + 1)) {
            const monthStart = new Date(cursor);
            const key = `${monthStart.getFullYear()}-${String(monthStart.getMonth() + 1).padStart(2, '0')}`;
            groups[key] = {
                date: `${monthStart.getMonth() + 1}月`,
                inputToken: 0,
                outputToken: 0,
                totalToken: 0,
                key
            };
        }
    }

    points.forEach(point => {
        const parsedDate = new Date(point.date);
        if (Number.isNaN(parsedDate.getTime())) return;
        const recordDate = normalizeDate(parsedDate);
        let key;
        let label;

        if (period === 'week') {
            const weekStart = getWeekStart(recordDate);
            const weekEnd = new Date(weekStart);
            weekEnd.setDate(weekStart.getDate() + 6);
            key = `${weekStart.getFullYear()}-${String(weekStart.getMonth() + 1).padStart(2, '0')}-${String(weekStart.getDate()).padStart(2, '0')}`;
            label = `${weekStart.getMonth() + 1}/${weekStart.getDate()}-${weekEnd.getMonth() + 1}/${weekEnd.getDate()}`;
        } else {
            key = `${recordDate.getFullYear()}-${String(recordDate.getMonth() + 1).padStart(2, '0')}`;
            label = `${recordDate.getMonth() + 1}月`;
        }

        if (!groups[key]) {
            groups[key] = { date: label, inputToken: 0, outputToken: 0, totalToken: 0, key };
        }
        groups[key].inputToken += Number(point.inputToken || 0);
        groups[key].outputToken += Number(point.outputToken || 0);
        groups[key].totalToken += Number(point.totalToken || 0);
    });

    return Object.values(groups).sort((a, b) => a.key.localeCompare(b.key));
}

function buildServerDailyTrend(points) {
    return points.map(point => {
        const parsedDate = new Date(point.date);
        const label = Number.isNaN(parsedDate.getTime())
            ? point.date
            : `${parsedDate.getMonth() + 1}/${parsedDate.getDate()}`;
        return {
            date: label,
            inputToken: Number(point.inputToken || 0),
            outputToken: Number(point.outputToken || 0),
            totalToken: Number(point.totalToken || 0)
        };
    });
}

function getLatestHistoricalTime() {
    const records = tokenData?.records || [];
    const dates = records
        .map(record => parseMockDate(record.callTime))
        .filter(Boolean)
        .sort((a, b) => b.getTime() - a.getTime());
    if (!dates.length) return '';
    return formatCallTime(dates[0]);
}

function ensureTrendCanvas() {
    const chartCard = document.querySelector('.chart-card');
    if (!chartCard) return null;

    let canvas = document.getElementById('trendChart');
    if (!canvas) {
        chartCard.innerHTML = '<canvas id="trendChart"></canvas>';
        canvas = document.getElementById('trendChart');
    }
    return canvas;
}

function renderTrendEmptyState(message) {
    if (trendChart) {
        trendChart.destroy();
        trendChart = null;
    }

    const chartCard = document.querySelector('.chart-card');
    if (!chartCard) return;
    chartCard.innerHTML = `<div class="empty-table">${message}</div>`;
}

function renderChart() {
    if (typeof Chart === 'undefined') {
        renderTrendEmptyState('趋势图资源加载失败，统计卡片和记录表仍可正常查看。');
        return;
    }
    let data = [];
    // Use the complete daily series returned by the real trend API, then
    // aggregate those points for the weekly and monthly views.
    if (Array.isArray(tokenTrendData)) {
        if (currentPeriod === 'week') {
            data = buildServerTrendByPeriod(tokenTrendData, 'week');
        } else if (currentPeriod === 'month') {
            data = buildServerTrendByPeriod(tokenTrendData, 'month');
        } else {
            data = buildServerDailyTrend(tokenTrendData);
        }
    }

    const hasTrendValues = data.some(item => (item.totalToken || item.inputToken || item.outputToken || 0) > 0);
    if (!data.length || !hasTrendValues) {
        const latestTime = getLatestHistoricalTime();
        const suffix = latestTime ? `，当前历史记录最新时间为 ${latestTime}` : '';
        renderTrendEmptyState(`当前筛选时间范围内暂无趋势数据${suffix}。`);
        return;
    }

    const canvas = ensureTrendCanvas();
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    
    const labels = data.map(d => d.date);
    const inputData = data.map(d => d.inputToken);
    const outputData = data.map(d => d.outputToken);
    const totalData = data.map(d => d.totalToken);
    
    if (trendChart) {
        trendChart.destroy();
    }
    
    trendChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                        {
                            label: '输入Token',
                            data: inputData,
                            borderColor: '#2f65ff',
                            backgroundColor: 'rgba(47,101,255,0.10)',
                            fill: true,
                            tension: 0.36,
                            pointRadius: 3,
                            pointHoverRadius: 5
                        },
                        {
                            label: '输出Token',
                            data: outputData,
                            borderColor: '#6fb0ff',
                            backgroundColor: 'rgba(111,176,255,0.08)',
                            fill: true,
                            tension: 0.36,
                            pointRadius: 3,
                            pointHoverRadius: 5
                        },
                        {
                            label: '总消耗',
                            data: totalData,
                            borderColor: '#1f4dc6',
                            backgroundColor: 'rgba(31,77,198,0.14)',
                            fill: true,
                            tension: 0.36,
                            pointRadius: 4,
                            pointHoverRadius: 6,
                            borderWidth: 2
                        }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        usePointStyle: true,
                        padding: 20,
                        color: '#1f3a70'
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(255, 255, 255, 0.96)',
                    titleColor: '#1f3a70',
                    bodyColor: '#4b6f9f',
                    borderColor: '#dbe6f7',
                    borderWidth: 1,
                    padding: 14,
                    displayColors: true,
                    callbacks: {
                        label: function(context) {
                            const label = context.dataset.label || '';
                            const value = context.parsed.y;
                            return `${label}: ${value.toLocaleString('zh-CN')}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#6b7f9d',
                        maxRotation: 45,
                        minRotation: 0
                    }
                },
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(41, 102, 200, 0.08)'
                    },
                    ticks: {
                        color: '#6b7f9d',
                        callback: function(value) {
                            if (value >= 1000) {
                                return (value / 1000).toFixed(0) + 'k';
                            }
                            return value;
                        }
                    }
                }
            }
        }
    });
}

function renderTable() {
    // Support server-side pagination when available
    if (tokenData?.fromServer) {
        const currentRecords = tokenData.records || [];
        const totalRecords = tokenData.totalElements || (tokenData.totalPages ? tokenData.totalPages * (tokenData.size || itemsPerPage) : currentRecords.length);
        const totalPages = tokenData.totalPages || Math.ceil(totalRecords / (tokenData.size || itemsPerPage));

        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;
        const startIndex = (currentPage - 1) * (tokenData.size || itemsPerPage);
        const endIndex = startIndex + (tokenData.size || itemsPerPage);
        // render currentRecords directly
        const tbody = document.getElementById('recordTableBody');
        const tableCard = document.querySelector('.table-card');
        const emptyState = document.getElementById('emptyState');
        const metaText = document.querySelector('.table-header .meta');

        if (!currentRecords.length) {
            tableCard.style.display = 'none';
            emptyState.style.display = 'block';
        } else {
            tableCard.style.display = 'block';
            emptyState.style.display = 'none';

            tbody.innerHTML = currentRecords.map(record => `
                <tr data-id="${record.id}">
                    <td>${record.callTime}</td>
                    <td>${record.modelName}</td>
                    <td>${(record.inputToken||0).toLocaleString('zh-CN')}</td>
                    <td>${(record.outputToken||0).toLocaleString('zh-CN')}</td>
                    <td>${(record.totalToken||0).toLocaleString('zh-CN')}</td>
                    <td>
                        <span class="status-badge ${record.status}">
                            ${record.status === 'success' ? '✓ 成功' : record.status === 'failed' ? '✗ 失败' : '⏳ 处理中'}
                        </span>
                    </td>
                    <td>
                        <button class="detail-btn" onclick="showDetail('${record.id}')">查看详情</button>
                    </td>
                </tr>
            `).join('');

            if (metaText) {
                const displayStart = startIndex + 1;
                const displayEnd = Math.min(endIndex, totalRecords);
                metaText.textContent = `显示 ${displayStart} 到 ${displayEnd} 条，共 ${totalRecords} 条记录`;
            }
        }

        renderPagination(totalPages);
        return;
    }

    // fallback: client-side filtering and pagination
    filteredRecords = getFilteredRecords();
    const totalRecords = filteredRecords.length;
    const totalPages = Math.ceil(totalRecords / itemsPerPage);

    if (currentPage > totalPages && totalPages > 0) {
        currentPage = totalPages;
    }

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const currentRecords = filteredRecords.slice(startIndex, endIndex);
    const tbody = document.getElementById('recordTableBody');
    const tableCard = document.querySelector('.table-card');
    const emptyState = document.getElementById('emptyState');
    const metaText = document.querySelector('.table-header .meta');

    if (totalRecords === 0) {
        tableCard.style.display = 'none';
        emptyState.style.display = 'block';
    } else {
        tableCard.style.display = 'block';
        emptyState.style.display = 'none';

        tbody.innerHTML = currentRecords.map(record => `
            <tr data-id="${record.id}">
                <td>${record.callTime}</td>
                <td>${record.modelName}</td>
                <td>${record.inputToken.toLocaleString('zh-CN')}</td>
                <td>${record.outputToken.toLocaleString('zh-CN')}</td>
                <td>${record.totalToken.toLocaleString('zh-CN')}</td>
                <td>
                    <span class="status-badge ${record.status}">
                        ${record.status === 'success' ? '✓ 成功' : record.status === 'failed' ? '✗ 失败' : '⏳ 处理中'}
                    </span>
                </td>
                <td>
                    <button class="detail-btn" onclick="showDetail('${record.id}')">查看详情</button>
                </td>
            </tr>
        `).join('');

        if (metaText) {
            const displayStart = startIndex + 1;
            const displayEnd = Math.min(endIndex, totalRecords);
            metaText.textContent = `显示 ${displayStart} 到 ${displayEnd} 条，共 ${totalRecords} 条记录`;
        }
    }
    
    renderPagination(totalPages);
}

function renderPagination(totalPages) {
    const pagination = document.getElementById('pagination');
    
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = `
        <button class="pagination-btn" ${currentPage === 1 ? 'disabled' : ''} onclick="changePage(${currentPage - 1})">上一页</button>
    `;
    
    const maxVisible = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages, startPage + maxVisible - 1);
    
    if (endPage - startPage + 1 < maxVisible) {
        startPage = Math.max(1, endPage - maxVisible + 1);
    }
    
    if (startPage > 1) {
        html += `<button class="pagination-btn" onclick="changePage(1)">1</button>`;
        if (startPage > 2) {
            html += `<span class="pagination-dots">...</span>`;
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        html += `
            <button class="pagination-btn ${i === currentPage ? 'active' : ''}" onclick="changePage(${i})">${i}</button>
        `;
    }
    
    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            html += `<span class="pagination-dots">...</span>`;
        }
        html += `<button class="pagination-btn" onclick="changePage(${totalPages})">${totalPages}</button>`;
    }
    
    html += `
        <button class="pagination-btn" ${currentPage === totalPages ? 'disabled' : ''} onclick="changePage(${currentPage + 1})">下一页</button>
    `;
    
    pagination.innerHTML = html;
}

function changePage(page) {
    currentPage = page;
    // if server-side pagination, request the page
    if (tokenData?.fromServer) {
        const userId = getCustomerId();
        const size = tokenData.size || itemsPerPage;
        window.tokenApi.getRecords(userId, { page: currentPage, size }).then(res => {
            const items = res?.items || res?.data?.items || res?.data || res || [];
            const mapped = Array.isArray(items) ? items.map(normalizeRecord) : [];
            tokenData.records = mapped;
            tokenData.page = res?.page || currentPage;
            tokenData.size = res?.size || size;
            tokenData.totalElements = res?.totalElements || tokenData.totalElements || 0;
            tokenData.totalPages = res?.totalPages || tokenData.totalPages || Math.ceil((tokenData.totalElements || 0) / (tokenData.size || size));
            renderTable();
        }).catch(err => {
            console.warn('Failed to load page', err);
            // fallback to client-side render
            renderTable();
        });
        return;
    }
    renderTable();
}

function showDetail(id) {
    let record = (tokenData?.records || []).find(r => r.id === id);
    if (!record && window.tokenApi && window.tokenApi.getRecordDetail) {
        // try fetch from server
        try {
            const userId = getCustomerId();
            const res = window.tokenApi.getRecordDetail(userId, id);
            // res may be a promise
            if (res && typeof res.then === 'function') {
                res.then(r => {
                    const rec = normalizeRecord(r?.data || r || {});
                    displayDetail(rec);
                }).catch(() => {});
                return;
            } else {
                record = normalizeRecord(res?.data || res || {});
            }
        } catch (e) {
            return;
        }
    }
    if (!record) return;
    
    const modal = document.getElementById('detailModal');
    const modalBody = document.getElementById('modalBody');
    
    function displayDetail(rec) {
        if (!rec) return;
        const r = rec;
        modalBody.innerHTML = `
            <div class="detail-row">
                <span class="detail-label">记录ID</span>
                <span class="detail-value">${r.id}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">调用时间</span>
                <span class="detail-value">${r.callTime}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">模型名称</span>
                <span class="detail-value">${r.modelName}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">会话ID</span>
                <span class="detail-value">${r.sessionId}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">用户ID</span>
                <span class="detail-value">${r.userId}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">输入Token</span>
                <span class="detail-value">${(r.inputToken||0).toLocaleString('zh-CN')}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">输出Token</span>
                <span class="detail-value">${(r.outputToken||0).toLocaleString('zh-CN')}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">总消耗</span>
                <span class="detail-value">${(r.totalToken||0).toLocaleString('zh-CN')}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">响应时间</span>
                <span class="detail-value">${r.responseTime}ms</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">状态</span>
                <span class="detail-value">
                    <span class="status-badge ${r.status}">
                        ${r.status === 'success' ? '✓ 成功' : r.status === 'failed' ? '✗ 失败' : '⏳ 处理中'}
                    </span>
                </span>
            </div>
            <div class="detail-row">
                <span class="detail-label">用户输入</span>
                <span class="detail-value" style="text-align: right; max-width: 60%; word-break: break-all;">${r.prompt}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">模型响应</span>
                <span class="detail-value" style="text-align: right; max-width: 60%; word-break: break-all;">${r.response}</span>
            </div>
        `;
        modal.classList.add('active');
    }

    if (record) displayDetail(record);
}

function closeModal() {
    const modal = document.getElementById('detailModal');
    modal.classList.remove('active');
}

function bindEvents() {
    if (eventsBound) return;
    eventsBound = true;

    const dashboardToggle = document.getElementById('dashboardSectionToggle');
    const tokenStatsMenuItem = document.getElementById('tokenStatsMenuItem');

    // ============【重点改造：绑定折叠事件，调用统一toggleExpand】============
    if (dashboardToggle) {
        dashboardToggle.addEventListener('click', function() {
            toggleExpand("dashboard");
        });
    }

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentPeriod = this.dataset.period;
            loadTrend().then(renderChart).catch(() => renderChart());
        });
    });
    
    document.getElementById('closeModal').addEventListener('click', closeModal);
    
    document.getElementById('detailModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeModal();
        }
    });
    
    document.getElementById('retryBtn').addEventListener('click', function() {
        hideErrorState();
        initApp();
    });
    
    // Keep compatibility when top menu button is absent.
    const menuToggle = document.querySelector('.menu-toggle');
    if (menuToggle) {
        menuToggle.addEventListener('click', function() {
            return;
        });
    }

    // sidebar menu item activation
    const customerNavMap = getCustomerNavMap();

    document.querySelectorAll('.sidebar .menu-item').forEach(item => {
        item.addEventListener('click', function() {
            if (this.id === 'dashboardSectionToggle') return;

            const label = this.querySelector('.label')?.textContent?.trim();
            const target = label ? customerNavMap[label] : '';
            if (target) {
                window.location.href = target;
                return;
            }

            document.querySelectorAll('.sidebar .menu-item').forEach(i => i.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

function showErrorState() {
    document.getElementById('errorState').style.display = 'block';
    document.querySelector('.page-content').style.display = 'none';
}

function hideErrorState() {
    document.getElementById('errorState').style.display = 'none';
    document.querySelector('.page-content').style.display = 'block';
}


// ===== account dropdown + logout (unified with workbench) =====
let userMenuOpen = false;
let accountModalType = '';
let accountSubmitting = false;

function bindAccountMenu() {
    const wrap = document.getElementById('userProfileWrap');
    const button = document.getElementById('userProfileButton');
    const dropdown = document.getElementById('userDropdown');
    const modal = document.getElementById('accountConfirmModal');
    const openKey = button ? button.querySelector('.open-key') : null;
    if (!button || !dropdown || !modal) return;

    button.addEventListener('click', function (event) {
        event.stopPropagation();
        userMenuOpen = !userMenuOpen;
        dropdown.style.display = userMenuOpen ? 'block' : 'none';
        if (openKey) openKey.classList.toggle('is-open', userMenuOpen);
    });

    document.addEventListener('click', function () {
        userMenuOpen = false;
        dropdown.style.display = 'none';
        if (openKey) openKey.classList.remove('is-open');
    });

    dropdown.querySelectorAll('button').forEach(function (item) {
        item.addEventListener('click', function (event) {
            event.stopPropagation();
            const action = item.dataset.action;
            if (action === 'logout' || action === 'switch') {
                accountModalType = action;
                document.getElementById('accountConfirmTitle').textContent =
                    action === 'logout' ? "确认退出登录" : "确认切换账号";
                modal.style.display = 'flex';
            }
            userMenuOpen = false;
            dropdown.style.display = 'none';
            if (openKey) openKey.classList.remove('is-open');
        });
    });

    document.getElementById('accountConfirmCancel').addEventListener('click', function () {
        modal.style.display = 'none';
    });

    document.getElementById('accountConfirmOk').addEventListener('click', function () {
        if (accountSubmitting) return;
        accountSubmitting = true;
        document.getElementById('accountConfirmOk').textContent = "处理中...";
        logoutAndRedirect(window.location.href).finally(function () {
            accountSubmitting = false;
        });
    });
}

function readCookie(name) {
    const cookie = document.cookie.split('; ').find(function (item) { return item.startsWith(name + '='); });
    return cookie ? decodeURIComponent(cookie.slice(name.length + 1)) : '';
}

async function ensureCsrfToken() {
    if (readCookie('XSRF-TOKEN')) return readCookie('XSRF-TOKEN');
    await fetch('/api/v1/public/login-config', { credentials: 'include' });
    return readCookie('XSRF-TOKEN');
}

function logoutAndRedirect(returnUrl) {
    const local = window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost';
    const loginUrl = local
        ? window.location.protocol + '//' + window.location.hostname + ':15173/'
        : window.location.origin + '/';
    return ensureCsrfToken()
        .then(function (csrfToken) {
            return fetch('/api/v1/auth/logout', {
                method: 'POST',
                credentials: 'include',
                headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : undefined
            }).catch(function () {});
        })
        .finally(function () {
            const url = new URL(loginUrl, window.location.origin);
            url.searchParams.set('returnUrl', returnUrl);
            window.location.replace(url.toString());
        });
}

function startApp() {
    bindEvents();
    bindAccountMenu();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initApp);
    } else {
        initApp();
    }
}

startApp();
