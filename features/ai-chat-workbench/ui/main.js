const BASE_URL = 'http://localhost:8081/api/v1';
const CUSTOMER_ID = 'test_customer';

let currentConversationId = null;
let currentPlatform = '';
let generateRequest = null;
let fiveSecondTimer = null;
let fifteenSecondTimer = null;
let favoritedScripts = new Map();

const stepContentMap = {
    intentRecognition: 'intentContent',
    replyStrategy: 'strategyContent',
    recommendedScript: 'scriptContent',
    hookGuidance: 'hookContent',
    successClose: 'closeContent'
};

const stepTitleMap = {
    intentRecognition: '意图识别',
    replyStrategy: '回复策略',
    recommendedScript: '推荐话术',
    hookGuidance: '钩子引导',
    successClose: '成功收尾'
};

document.addEventListener('DOMContentLoaded', () => {
    initEventListeners();
    loadTokenUsage();
    loadConversations();
});

function initEventListeners() {
    const platformSelect = document.getElementById('platformSelect');
    const questionInput = document.getElementById('questionInput');
    const charCount = document.getElementById('charCount');
    const generateBtn = document.getElementById('generateBtn');

    platformSelect.addEventListener('change', (e) => {
        currentPlatform = e.target.value;
        validateForm();
    });

    questionInput.addEventListener('input', () => {
        const count = questionInput.value.length;
        charCount.textContent = count;
        if (count > 500) {
            questionInput.value = questionInput.value.substring(0, 500);
            charCount.textContent = 500;
        }
        validateForm();
    });

    generateBtn.addEventListener('click', generateAIResponse);
}

function validateForm() {
    const platform = document.getElementById('platformSelect').value;
    const question = document.getElementById('questionInput').value.trim();
    const generateBtn = document.getElementById('generateBtn');
    const platformError = document.getElementById('platformError');
    const questionError = document.getElementById('questionError');

    let isValid = true;

    if (!platform) {
        platformError.textContent = '请先选择服务平台';
        isValid = false;
    } else {
        platformError.textContent = '';
    }

    if (!question) {
        questionError.textContent = '请输入客户问题';
        isValid = false;
    } else {
        questionError.textContent = '';
    }

    generateBtn.disabled = !isValid;
}

async function generateAIResponse() {
    const platform = document.getElementById('platformSelect').value;
    const question = document.getElementById('questionInput').value.trim();

    if (!platform || !question) {
        validateForm();
        return;
    }

    const generateBtn = document.getElementById('generateBtn');
    const loadingState = document.getElementById('loadingState');
    const loadingHint = document.getElementById('loadingHint');
    const errorMessage = document.getElementById('errorMessage');

    generateBtn.style.display = 'none';
    loadingState.style.display = 'flex';
    errorMessage.classList.remove('show');

    loadingHint.textContent = '';

    fiveSecondTimer = setTimeout(() => {
        loadingHint.textContent = 'AI正在努力生成回复，请稍候';
    }, 5000);

    fifteenSecondTimer = setTimeout(() => {
        if (generateRequest) {
            generateRequest.abort();
        }
        showError('AI生成失败，请稍后重试', true);
    }, 15000);

    try {
        const controller = new AbortController();
        generateRequest = controller;

        let url = `${BASE_URL}/conversations`;
        let method = 'POST';

        if (currentConversationId) {
            url = `${BASE_URL}/conversations/${currentConversationId}/messages`;
        }

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'X-Customer-Id': CUSTOMER_ID
            },
            body: JSON.stringify({
                question: question,
                platform: platform,
                customerType: '售前咨询'
            }),
            signal: controller.signal
        });

        clearTimers();

        if (!response.ok) {
            const errorData = await response.json();
            if (response.status === 403) {
                showError('今日Token配额已耗尽');
            } else {
                showError(errorData.message || '生成失败');
            }
            return;
        }

        const data = await response.json();
        currentConversationId = data.data.conversationId;
        currentPlatform = data.data.platform;
        displayConversation(data.data);
        loadConversations();
        loadTokenUsage();
        document.getElementById('questionInput').value = '';
        document.getElementById('charCount').textContent = '0';
        validateForm();

    } catch (error) {
        clearTimers();
        if (error.name !== 'AbortError') {
            showError('AI生成失败，请稍后重试', true);
        }
    }
}

function startNewConversation() {
    currentConversationId = null;
    currentPlatform = '';
    document.getElementById('platformSelect').value = '';
    document.getElementById('questionInput').value = '';
    document.getElementById('charCount').textContent = '0';
    document.getElementById('resultSection').style.display = 'none';
    document.getElementById('conversationMessages').innerHTML = '';
    validateForm();

    document.querySelectorAll('.conversation-item').forEach(item => {
        item.classList.remove('active');
    });
}

function clearTimers() {
    if (fiveSecondTimer) {
        clearTimeout(fiveSecondTimer);
        fiveSecondTimer = null;
    }
    if (fifteenSecondTimer) {
        clearTimeout(fifteenSecondTimer);
        fifteenSecondTimer = null;
    }
}

function showError(message, showRetry = false) {
    const generateBtn = document.getElementById('generateBtn');
    const loadingState = document.getElementById('loadingState');
    const errorMessage = document.getElementById('errorMessage');

    loadingState.style.display = 'none';
    generateBtn.style.display = 'flex';

    if (showRetry) {
        errorMessage.innerHTML = `${message} <button onclick="generateAIResponse()">重新生成</button>`;
    } else {
        errorMessage.textContent = message;
    }
    errorMessage.classList.add('show');
}

function displayConversation(data) {
    const loadingState = document.getElementById('loadingState');
    const generateBtn = document.getElementById('generateBtn');
    const resultSection = document.getElementById('resultSection');

    loadingState.style.display = 'none';
    generateBtn.style.display = 'flex';

    document.getElementById('platformSelect').value = data.platform;
    currentPlatform = data.platform;

    document.getElementById('riskPlatform').textContent = data.messages && data.messages.length > 0 
        ? data.messages[data.messages.length - 1].riskWarning || '' : '';
    document.getElementById('riskSuggestion').textContent = data.messages && data.messages.length > 0 
        ? data.messages[data.messages.length - 1].riskSuggestion || '' : '';

    if (!document.getElementById('riskPlatform').textContent && !document.getElementById('riskSuggestion').textContent) {
        document.getElementById('riskWarning').style.display = 'none';
    } else {
        document.getElementById('riskWarning').style.display = 'block';
    }

    const messagesContainer = document.getElementById('conversationMessages');
    if (data.messages && data.messages.length > 0) {
        messagesContainer.innerHTML = data.messages.map((msg, index) => renderMessageTurn(msg, index + 1)).join('');
    } else {
        messagesContainer.innerHTML = '<div class="empty-state">暂无消息</div>';
    }

    resultSection.style.display = 'block';

    updateFavoriteButtonStates();
}

function renderMessageTurn(message, index) {
    return `
        <div class="message-turn">
            <div class="message-header">
                <span class="message-index">第 ${index} 轮对话</span>
                <span class="message-time">${formatTime(message.createdAt)}</span>
            </div>
            <div class="question-section">
                <div class="question-label">客户问题</div>
                <div class="question-text">${message.question}</div>
            </div>
            <div class="five-step-framework">
                <div class="step-card" data-step="intentRecognition" data-message-id="${message.id}">
                    <div class="step-header">
                        <h3>意图识别</h3>
                        <button class="favorite-btn" onclick="toggleFavorite('intentRecognition', ${message.id}, '${escapeHtml(message.intentRecognition || '')}', '${message.platform || ''}', '${message.customerType || ''}', '${escapeHtml(message.question || '')}')">
                            <span class="favorite-icon">★</span>
                            <span class="favorite-text">收藏</span>
                        </button>
                    </div>
                    <div class="step-content">${message.intentRecognition || '暂无内容'}</div>
                </div>
                <div class="step-card" data-step="replyStrategy" data-message-id="${message.id}">
                    <div class="step-header">
                        <h3>回复策略</h3>
                        <button class="favorite-btn" onclick="toggleFavorite('replyStrategy', ${message.id}, '${escapeHtml(message.replyStrategy || '')}', '${message.platform || ''}', '${message.customerType || ''}', '${escapeHtml(message.question || '')}')">
                            <span class="favorite-icon">★</span>
                            <span class="favorite-text">收藏</span>
                        </button>
                    </div>
                    <div class="step-content">${message.replyStrategy || '暂无内容'}</div>
                </div>
                <div class="step-card" data-step="recommendedScript" data-message-id="${message.id}">
                    <div class="step-header">
                        <h3>推荐话术</h3>
                        <button class="favorite-btn" onclick="toggleFavorite('recommendedScript', ${message.id}, '${escapeHtml(message.recommendedScript || '')}', '${message.platform || ''}', '${message.customerType || ''}', '${escapeHtml(message.question || '')}')">
                            <span class="favorite-icon">★</span>
                            <span class="favorite-text">收藏到话术库</span>
                        </button>
                    </div>
                    <div class="step-content">${message.recommendedScript || '暂无内容'}</div>
                </div>
                <div class="step-card" data-step="hookGuidance" data-message-id="${message.id}">
                    <div class="step-header">
                        <h3>钩子引导</h3>
                        <button class="favorite-btn" onclick="toggleFavorite('hookGuidance', ${message.id}, '${escapeHtml(message.hookGuidance || '')}', '${message.platform || ''}', '${message.customerType || ''}', '${escapeHtml(message.question || '')}')">
                            <span class="favorite-icon">★</span>
                            <span class="favorite-text">收藏</span>
                        </button>
                    </div>
                    <div class="step-content">${message.hookGuidance || '暂无内容'}</div>
                </div>
                <div class="step-card" data-step="successClose" data-message-id="${message.id}">
                    <div class="step-header">
                        <h3>成功收尾</h3>
                        <button class="favorite-btn" onclick="toggleFavorite('successClose', ${message.id}, '${escapeHtml(message.successClose || '')}', '${message.platform || ''}', '${message.customerType || ''}', '${escapeHtml(message.question || '')}')">
                            <span class="favorite-icon">★</span>
                            <span class="favorite-text">收藏</span>
                        </button>
                    </div>
                    <div class="step-content">${message.successClose || '暂无内容'}</div>
                </div>
            </div>
        </div>
    `;
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

async function toggleFavorite(step, messageId, content, platform, customerType, question) {
    if (!currentConversationId) {
        showToast('请先生成AI回复', 'warning');
        return;
    }

    const btn = document.querySelector(`.step-card[data-step="${step}"][data-message-id="${messageId}"] .favorite-btn`);

    if (!content.trim()) {
        showToast('该步骤暂无内容', 'warning');
        return;
    }

    const hash = await generateHash(content);

    if (favoritedScripts.has(hash)) {
        await unfavoriteScript(hash, btn);
    } else {
        await favoriteScript(step, content, hash, btn, platform, customerType, question);
    }
}

async function generateHash(content) {
    const response = await fetch(`${BASE_URL}/favorites/hash`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ scriptContent: content })
    });
    const data = await response.json();
    return data.data;
}

async function favoriteScript(step, content, hash, btn, platform, customerType, question) {
    try {
        const response = await fetch(`${BASE_URL}/favorites`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Customer-Id': CUSTOMER_ID
            },
            body: JSON.stringify({
                scriptContent: content,
                conversationId: currentConversationId,
                platform: platform,
                customerType: customerType,
                scriptStep: step,
                question: question
            })
        });

        if (!response.ok) {
            if (response.status === 409) {
                showToast('该话术已被收藏', 'warning');
            } else {
                showToast('收藏失败', 'error');
            }
            return;
        }

        favoritedScripts.set(hash, step);
        updateFavoriteButton(btn, true);
        showToast('已收藏到话术库', 'success');

    } catch (error) {
        showToast('收藏失败', 'error');
    }
}

async function unfavoriteScript(hash, btn) {
    try {
        const response = await fetch(`${BASE_URL}/favorites`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'X-Customer-Id': CUSTOMER_ID
            },
            body: JSON.stringify({ scriptHash: hash })
        });

        if (!response.ok) {
            showToast('取消收藏失败', 'error');
            return;
        }

        favoritedScripts.delete(hash);
        updateFavoriteButton(btn, false);
        showToast('已取消收藏', 'success');

    } catch (error) {
        showToast('取消收藏失败', 'error');
    }
}

function updateFavoriteButton(btn, isFavorited) {
    if (isFavorited) {
        btn.classList.add('favorited');
        btn.querySelector('.favorite-text').textContent = '已收藏';
    } else {
        btn.classList.remove('favorited');
        btn.querySelector('.favorite-text').textContent = '收藏';
    }
}

async function updateFavoriteButtonStates() {
    favoritedScripts.clear();

    const response = await fetch(`${BASE_URL}/favorites`, {
        headers: { 'X-Customer-Id': CUSTOMER_ID }
    });
    const data = await response.json();

    if (data.data) {
        data.data.forEach(fav => {
            favoritedScripts.set(fav.scriptHash, fav.scriptStep);
        });
    }

    document.querySelectorAll('.step-card').forEach(card => {
        const step = card.dataset.step;
        const content = card.querySelector('.step-content').textContent;
        if (content && content !== '暂无内容') {
            generateHash(content).then(hash => {
                const btn = card.querySelector('.favorite-btn');
                if (btn && favoritedScripts.has(hash)) {
                    updateFavoriteButton(btn, true);
                }
            });
        }
    });
}

async function loadConversations() {
    try {
        const response = await fetch(`${BASE_URL}/conversations`, {
            headers: { 'X-Customer-Id': CUSTOMER_ID }
        });
        const data = await response.json();

        const list = document.getElementById('conversationList');
        if (!data.data || data.data.length === 0) {
            list.innerHTML = '<div class="empty-state">暂无对话记录</div>';
            return;
        }

        list.innerHTML = data.data.map(conv => `
            <div class="conversation-item ${conv.conversationId === currentConversationId ? 'active' : ''}" 
                 onclick="loadConversation('${conv.conversationId}')">
                <div class="question-preview">${conv.title || '无标题'}</div>
                <div class="conversation-meta">
                    <span class="platform-badge">${conv.platform}</span>
                    <span class="time-stamp">${formatTime(conv.updatedAt)}</span>
                </div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Failed to load conversations:', error);
    }
}

async function loadConversation(conversationId) {
    try {
        const response = await fetch(`${BASE_URL}/conversations/${conversationId}`, {
            headers: { 'X-Customer-Id': CUSTOMER_ID }
        });

        if (!response.ok) {
            showToast('加载对话失败', 'error');
            return;
        }

        const data = await response.json();
        currentConversationId = data.data.conversationId;
        currentPlatform = data.data.platform;

        document.getElementById('platformSelect').value = data.data.platform;
        document.getElementById('questionInput').value = '';
        document.getElementById('charCount').textContent = '0';

        validateForm();
        displayConversation(data.data);

        document.querySelectorAll('.conversation-item').forEach(item => {
            item.classList.remove('active');
            if (item.getAttribute('onclick') && item.getAttribute('onclick').includes(conversationId)) {
                item.classList.add('active');
            }
        });

    } catch (error) {
        showToast('加载对话失败', 'error');
    }
}

async function loadTokenUsage() {
    try {
        const response = await fetch(`${BASE_URL}/tokens/usage`, {
            headers: { 'X-Customer-Id': CUSTOMER_ID }
        });
        const data = await response.json();

        const used = data.data.usedToday || 0;
        const limit = data.data.totalLimit || 50000;
        const percent = (used / limit) * 100;

        document.getElementById('tokenProgressBar').style.width = `${percent}%`;
        document.getElementById('tokenText').textContent = `${used}/${limit}`;

        document.getElementById('tokenUsedToday').textContent = used;
        document.getElementById('tokenUsagePercent').textContent = `${percent.toFixed(1)}%`;
        document.getElementById('tokenRemaining').textContent = limit - used;

    } catch (error) {
        console.error('Failed to load token usage:', error);
    }
}

function showTokenModal() {
    document.getElementById('tokenModal').classList.add('show');
    loadTokenUsage();
}

function closeTokenModal() {
    document.getElementById('tokenModal').classList.remove('show');
}

async function showScriptLibrary() {
    document.getElementById('scriptLibraryModal').classList.add('show');
    await loadScriptLibrary();
}

function closeScriptLibrary() {
    document.getElementById('scriptLibraryModal').classList.remove('show');
}

async function loadScriptLibrary() {
    try {
        const response = await fetch(`${BASE_URL}/favorites`, {
            headers: { 'X-Customer-Id': CUSTOMER_ID }
        });
        const data = await response.json();

        const content = document.getElementById('scriptLibraryContent');
        if (!data.data || data.data.length === 0) {
            content.innerHTML = '<div class="empty-state">暂无收藏的话术</div>';
            return;
        }

        content.innerHTML = data.data.map(fav => `
            <div class="script-library-item">
                <div class="script-header">
                    <div>
                        <span class="script-step">${stepTitleMap[fav.scriptStep] || fav.scriptStep}</span>
                        <span class="script-platform">${fav.platform}</span>
                    </div>
                    <button class="unfavorite-btn" onclick="removeFromLibrary('${fav.scriptHash}')">取消收藏</button>
                </div>
                <div class="script-content">${fav.scriptContent}</div>
                <div class="script-meta">问题：${fav.question || '无'} | ${formatTime(fav.createdAt)}</div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Failed to load script library:', error);
    }
}

async function removeFromLibrary(hash) {
    try {
        const response = await fetch(`${BASE_URL}/favorites`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'X-Customer-Id': CUSTOMER_ID
            },
            body: JSON.stringify({ scriptHash: hash })
        });

        if (!response.ok) {
            showToast('取消收藏失败', 'error');
            return;
        }

        showToast('已取消收藏', 'success');
        await loadScriptLibrary();
        await updateFavoriteButtonStates();

    } catch (error) {
        showToast('取消收藏失败', 'error');
    }
}

function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3000);
}

function formatTime(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;

    return date.toLocaleDateString('zh-CN', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            overlay.classList.remove('show');
        }
    });
});
