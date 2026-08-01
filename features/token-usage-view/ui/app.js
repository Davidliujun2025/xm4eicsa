let trendChart = null;
let tokenData = null;
let tokenUsageInfo = null;
let tokenTrendData = null;
let currentPeriod = 'day';
let currentFilter = {
    type: 'range',
    range: 7,
    startDate: null,
    endDate: null
};
let currentPage = 1;
let itemsPerPage = 10;
let filteredRecords = [];

function getCustomerId() {
    return new URLSearchParams(window.location.search).get('customerId') || localStorage.getItem('customerId') || 'demo-customer';
}

async function loadTokenData() {
    // Prefer backend records via tokenApi with server pagination, fallback to mockData
    const userId = getCustomerId();
    try {
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
        updateDataSourceNote('后端记录数据');
        return tokenData;
    } catch (error) {
        updateDataSourceNote('本地模拟数据');
        // normalize mock data structure if present
        const mockRecords = (typeof mockData !== 'undefined' && Array.isArray(mockData.records)) ? mockData.records.map(normalizeRecord) : (typeof mockData !== 'undefined' && Array.isArray(mockData) ? mockData.map(normalizeRecord) : []);
        tokenData = { records: mockRecords, fromServer: false };
        return tokenData;
    }
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
                updateDataSourceNote('后端TokenUsage');
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
                updateDataSourceNote('后端Summary');
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
            const res = await window.tokenApi.getTrend(userId, { range: currentFilter.range === 30 ? 'LAST_30_DAYS' : 'LAST_7_DAYS' });
            const data = res?.data || res || null;
            if (Array.isArray(data)) {
                tokenTrendData = data.map(normalizeTrendPoint).filter(Boolean);
            } else if (data && Array.isArray(data.items)) {
                tokenTrendData = data.items.map(normalizeTrendPoint).filter(Boolean);
            } else {
                tokenTrendData = null;
            }
            if (tokenTrendData) updateDataSourceNote('后端Trend');
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
            tokenUsageInfo = payload;
            renderStats();
        }
        if (evt.recordAdded || evt.newRecord) {
            loadTokenData().then(() => { renderTable(); renderChart(); }).catch(()=>{});
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

function updateDataSourceNote(source) {
    const note = document.getElementById('dataSourceNote');
    if (note) {
        let detail = '';
        try {
            const base = window.tokenApi && window.tokenApi.getBaseURL ? window.tokenApi.getBaseURL() : (window.TOKEN_API_BASE_URL || '');
            if (base && !/模拟|本地|mock/i.test((source||''))) {
                detail = `（后端：${base}）`;
            }
        } catch (e) {}
        note.textContent = `当前数据来源：${source} ${detail}`.trim();
    }
    const banner = document.getElementById('mockBanner');
    if (banner) {
        const s = (source || '').toString();
        if (/模拟|本地|mock/i.test(s)) {
            banner.style.display = 'block';
        } else {
            banner.style.display = 'none';
        }
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
    if (currentFilter.type === 'custom' && currentFilter.startDate && currentFilter.endDate) {
        return {
            start: normalizeDate(currentFilter.startDate),
            end: normalizeDate(currentFilter.endDate)
        };
    }
    const end = getLatestRecordDate();
    const start = new Date(end);
    start.setDate(end.getDate() - currentFilter.range + 1);
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
    record.callTime = r.callTime || r.call_time || r.createdAt || r.created_at || r.timestamp || '';
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
    const refDate = getLatestRecordDate();
    const today = normalizeDate(refDate);
    const weekStart = getWeekStart(today);
    const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);

    const stats = {
        today: 0,
        week: 0,
        month: 0,
        history: 0
    };

    (tokenData?.records || []).forEach(record => {
        const recordDate = normalizeDate(parseMockDate(record.callTime));
        if (!recordDate) return;
        const value = record.totalToken || (record.inputToken + record.outputToken);
        stats.history += value;
        if (recordDate.getTime() === today.getTime()) {
            stats.today += value;
        }
        if (recordDate >= weekStart && recordDate <= today) {
            stats.week += value;
        }
        if (recordDate >= monthStart && recordDate <= today) {
            stats.month += value;
        }
    });

    if (tokenUsageInfo?.usedToday != null) {
        stats.today = tokenUsageInfo.usedToday;
    }

    return stats;
}

async function initApp() {
    try {
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
        // Start SSE only when records came from the backend. In local mock mode,
        // the static server has no /api/v1 stream endpoint.
        if (tokenData?.fromServer) {
            startStream();
        }
    } catch (error) {
        showErrorState();
        console.error('Failed to initialize app:', error);
    }
}

async function loadCurrentUser() {
    try {
        if (!window.tokenApi?.getCurrentUser) return;
        const payload = await window.tokenApi.getCurrentUser();
        const user = payload?.data || payload;
        const displayName = user?.username || user?.account || '客服';
        const nameElement = document.querySelector('.user-name');
        const avatarElement = document.querySelector('.avatar');
        if (nameElement) nameElement.textContent = displayName;
        if (avatarElement) avatarElement.textContent = displayName.trim().slice(0, 1) || '客';
    } catch (error) {
        // Keep the neutral page defaults if account details are unavailable.
    }
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

function renderChart() {
    if (typeof Chart === 'undefined') {
        const chartCard = document.querySelector('.chart-card');
        if (chartCard) {
            chartCard.innerHTML = '<div class="empty-table">趋势图资源加载失败，统计卡片和记录表仍可正常查看。</div>';
        }
        return;
    }
    const ctx = document.getElementById('trendChart').getContext('2d');
    let data = [];
    // Prefer server-provided trend data if available and period is day
    if (tokenTrendData && currentPeriod === 'day') {
        data = tokenTrendData.map(d => ({
            date: d.date || d.label || d.day,
            inputToken: d.inputToken || d.input_tokens || d.input || 0,
            outputToken: d.outputToken || d.output_tokens || d.output || 0,
            totalToken: d.totalToken || d.total_tokens || d.total || 0
        }));
    } else {
        const records = getFilteredRecords();
        switch (currentPeriod) {
            case 'week':
                data = buildTrendByPeriod(records, 'week');
                break;
            case 'month':
                data = buildTrendByPeriod(records, 'month');
                break;
            default:
                data = buildDailyTrend(records);
        }
    }
    
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
    
    const display = (rec) => {
        modalBody.innerHTML = `
        <div class="detail-row">
            <span class="detail-label">记录ID</span>
            <span class="detail-value">${record.id}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">调用时间</span>
            <span class="detail-value">${record.callTime}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">模型名称</span>
            <span class="detail-value">${record.modelName}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">会话ID</span>
            <span class="detail-value">${record.sessionId}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">用户ID</span>
            <span class="detail-value">${record.userId}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">输入Token</span>
            <span class="detail-value">${record.inputToken.toLocaleString('zh-CN')}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">输出Token</span>
            <span class="detail-value">${record.outputToken.toLocaleString('zh-CN')}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">总消耗</span>
            <span class="detail-value">${record.totalToken.toLocaleString('zh-CN')}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">响应时间</span>
            <span class="detail-value">${record.responseTime}ms</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">状态</span>
            <span class="detail-value">
                <span class="status-badge ${record.status}">
                    ${record.status === 'success' ? '✓ 成功' : record.status === 'failed' ? '✗ 失败' : '⏳ 处理中'}
                </span>
            </span>
        </div>
        <div class="detail-row">
            <span class="detail-label">用户输入</span>
            <span class="detail-value" style="text-align: right; max-width: 60%; word-break: break-all;">${record.prompt}</span>
        </div>
        <div class="detail-row">
            <span class="detail-label">模型响应</span>
            <span class="detail-value" style="text-align: right; max-width: 60%; word-break: break-all;">${record.response}</span>
        </div>
    `;
    };

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

    // if we had fetched record earlier, display immediately
    if (record) displayDetail(record);
}

function closeModal() {
    const modal = document.getElementById('detailModal');
    modal.classList.remove('active');
}

function bindEvents() {
    const dashboardToggle = document.getElementById('dashboardSectionToggle');
    const tokenStatsMenuItem = document.getElementById('tokenStatsMenuItem');
    const dashboardFoldStateKey = 'tokenDashboardFoldCollapsed';
    let dashboardCollapsed = false;

    const applyDashboardFoldState = () => {
        if (!dashboardToggle || !tokenStatsMenuItem) return;
        dashboardToggle.classList.toggle('collapsed', dashboardCollapsed);
        tokenStatsMenuItem.classList.toggle('hidden', dashboardCollapsed);
        dashboardToggle.classList.add('active');
        if (dashboardCollapsed) {
            tokenStatsMenuItem.classList.remove('active');
        } else {
            tokenStatsMenuItem.classList.add('active');
        }
    };

    if (dashboardToggle && tokenStatsMenuItem) {
        try {
            dashboardCollapsed = window.localStorage.getItem(dashboardFoldStateKey) === '1';
        } catch (e) {
            dashboardCollapsed = false;
        }
        applyDashboardFoldState();

        dashboardToggle.addEventListener('click', function() {
            dashboardCollapsed = !dashboardCollapsed;
            applyDashboardFoldState();
            try {
                window.localStorage.setItem(dashboardFoldStateKey, dashboardCollapsed ? '1' : '0');
            } catch (e) {
                // ignore localStorage write errors
            }
        });
    }

    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            const range = parseInt(this.dataset.range);
            const customPanel = document.getElementById('customRangePanel');
            if (range === 0) {
                currentFilter = { type: 'custom', range: 0, startDate: null, endDate: null };
                customPanel.style.display = 'flex';
                return;
            }
            customPanel.style.display = 'none';
            if (!isNaN(range)) {
                currentFilter = { type: 'range', range, startDate: null, endDate: null };
                currentPage = 1;
                currentPeriod = 'day';
                updateAll();
            }
        });
    });

    const applyCustom = document.getElementById('applyCustomRange');
    if (applyCustom) {
        applyCustom.addEventListener('click', function() {
            const start = document.getElementById('customStart').value;
            const end = document.getElementById('customEnd').value;
            if (!start || !end) return;
            const startDate = new Date(start);
            const endDate = new Date(end);
            const diffDays = Math.round((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;
            if (diffDays <= 0) return;
            currentFilter = {
                type: 'custom',
                range: diffDays,
                startDate,
                endDate
            };
            currentPage = 1;
            currentPeriod = 'day';
            updateAll();
        });
    }
    
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentPeriod = this.dataset.period;
            renderChart();
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
    const local = window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost';
    const localUrl = (port, path = '/') => `${window.location.protocol}//${window.location.hostname}:${port}${path}`;
    const customerNavMap = local ? {
        '智能对话': localUrl(15174),
        '对话记录': localUrl(15174, '/?view=history'),
        '个人话术库': localUrl(15278),
        '我的评估': localUrl(15174, '/?view=evaluation'),
        'Token统计': window.location.href
    } : {
        '智能对话': `${window.location.origin}/workbench/`,
        '对话记录': `${window.location.origin}/workbench/?view=history`,
        '个人话术库': `${window.location.origin}/favorite-script-library/`,
        '我的评估': `${window.location.origin}/workbench/?view=evaluation`,
        'Token统计': `${window.location.origin}/token-usage/`
    };

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
            if (dashboardToggle) {
                dashboardToggle.classList.add('active');
            }
            this.classList.add('active');
            if (this.id === 'tokenStatsMenuItem') {
                dashboardCollapsed = false;
                applyDashboardFoldState();
            }
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

function startApp() {
    bindEvents();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initApp);
    } else {
        initApp();
    }
}

startApp();
