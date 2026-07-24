let trendChart = null;
let tokenData = null;
let tokenUsageInfo = null;
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
    // TODO: 根据实际环境替换成真实 customerId 获取逻辑
    return new URLSearchParams(window.location.search).get('customerId') || localStorage.getItem('customerId') || 'demo-customer';
}

async function loadTokenData() {
    // 后端接入点：替换下面的 URL 为真实后端 API
    try {
        const response = await fetch('/api/token-usage');
        if (!response.ok) {
            throw new Error('Backend unavailable');
        }
        const data = await response.json();
        updateDataSourceNote('后端记录数据');
        return data;
    } catch (error) {
        // 本地开发时保留 mock 数据回退
        updateDataSourceNote('本地模拟数据');
        return typeof mockData !== 'undefined' ? mockData : { records: [] };
    }
}

async function loadTokenUsageInfo() {
    try {
        const customerId = getCustomerId();
        const response = await fetch('/api/v1/tokens/usage', {
            headers: {
                'X-Customer-Id': customerId
            }
        });
        if (!response.ok) {
            throw new Error('Token usage API unavailable');
        }
        const result = await response.json();
        const usageInfo = result?.data || null;
        if (usageInfo) {
            updateDataSourceNote('后端TokenUsage');
        }
        return usageInfo;
    } catch (error) {
        return null;
    }
}

function updateDataSourceNote(source) {
    const note = document.getElementById('dataSourceNote');
    if (note) {
        note.textContent = `当前数据来源：${source}`;
    }
}

function updateAll() {
    renderStats();
    renderChart();
    renderTable();
}

function parseMockDate(value) {
    if (!value) return null;
    if (value.includes('-')) {
        return new Date(value.replace(/-/g, '/'));
    }
    const parts = value.split('/').map(Number);
    if (parts.length >= 2) {
        return new Date(2026, parts[0] - 1, parts[1]);
    }
    return new Date(value);
}

function normalizeDate(date) {
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
        return recordDate >= start && recordDate <= end;
    });
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
        tokenData = await loadTokenData();
        tokenUsageInfo = await loadTokenUsageInfo();
        renderStats();
        renderChart();
        renderTable();
        bindEvents();
    } catch (error) {
        showErrorState();
        console.error('Failed to initialize app:', error);
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
    const ctx = document.getElementById('trendChart').getContext('2d');
    const records = getFilteredRecords();
    let data = [];
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
    renderTable();
}

function showDetail(id) {
    const record = (tokenData?.records || []).find(r => r.id === id);
    if (!record) return;
    
    const modal = document.getElementById('detailModal');
    const modalBody = document.getElementById('modalBody');
    
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
    
    modal.classList.add('active');
}

function closeModal() {
    const modal = document.getElementById('detailModal');
    modal.classList.remove('active');
}

function bindEvents() {
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
    
    // Sidebar collapse not required — menu-toggle is intentionally non-collapsing.
    document.querySelector('.menu-toggle').addEventListener('click', function() {
        // no-op: keep button visual but do not change layout
        return;
    });

    // sidebar menu item activation
    document.querySelectorAll('.sidebar .menu-item').forEach(item => {
        item.addEventListener('click', function() {
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

document.addEventListener('DOMContentLoaded', initApp);