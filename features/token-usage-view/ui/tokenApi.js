// tokenApi.js
// Simple wrapper for token usage APIs described in the spec.
// All functions use fetch and return the parsed JSON body (or throw on non-OK responses).

const _defaultBase = '/api/v1'; // adjust if you want a different default

function getBaseURL() {
  if (typeof window !== 'undefined') {
    if (window.TOKEN_API_BASE_URL) return normalizeBase(window.TOKEN_API_BASE_URL);
    try {
      const params = new URLSearchParams(window.location.search);
      const fromParam = params.get('apiBase');
      if (fromParam) return normalizeBase(fromParam);
    } catch (e) {
      // ignore
    }
  }
  return normalizeBase(_defaultBase);
}

// normalize and make tolerant: ensure no trailing slash and include /api/v1 when missing
function normalizeBase(raw) {
  if (!raw) return _defaultBase;
  let base = String(raw).trim();
  // remove trailing slash
  while (base.endsWith('/')) base = base.slice(0, -1);
  // if it doesn't contain an /api segment, append /api/v1
  if (!/\/api(\/|$)/i.test(base)) {
    base = base + '/api/v1';
  }
  return base;
}

async function checkResponse(res) {
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch (e) { /* not JSON */ }
  if (!res.ok) {
    const err = new Error(json?.error?.message || res.statusText || 'HTTP error');
    err.status = res.status;
    err.body = json || text;
    throw err;
  }
  return json;
}

// POST /api/v1/internal/token-usage/events
// internalKey required
async function postInternalEvent(eventPayload, internalKey) {
  const url = `${getBaseURL()}/internal/token-usage/events`;
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Internal-Api-Key': internalKey
    },
    body: JSON.stringify(eventPayload)
  });
  return await checkResponse(res);
}

// GET /api/v1/token-usage/me/today
async function getTodayStats(userId) {
  const url = `${getBaseURL()}/token-usage/me/today`;
  const res = await fetch(url, {
    credentials: 'include'
  });
  return await checkResponse(res);
}

// GET /api/v1/token-usage/me/summary?from=&to=
async function getSummary(userId, options = {}) {
  const params = new URLSearchParams();
  if (options.from) params.set('from', options.from);
  if (options.to) params.set('to', options.to);
  const url = `${getBaseURL()}/token-usage/me/summary${params.toString() ? '?' + params.toString() : ''}`;
  const res = await fetch(url, { credentials: 'include' });
  return await checkResponse(res);
}

// GET /api/v1/token-usage/me/trend?range=LAST_7_DAYS|LAST_30_DAYS|CUSTOM&from=&to=&bucket=
async function getTrend(userId, params = {}) {
  const q = new URLSearchParams();
  if (params.range) q.set('range', params.range);
  if (params.from) q.set('from', params.from);
  if (params.to) q.set('to', params.to);
  if (params.bucket) q.set('bucket', params.bucket);
  const url = `${getBaseURL()}/token-usage/me/trend?${q.toString()}`;
  const res = await fetch(url, { credentials: 'include' });
  return await checkResponse(res);
}

// GET /api/v1/token-usage/me/records?page=1&size=20&provider=&model=&from=&to=
async function getRecords(userId, options = {}) {
  const q = new URLSearchParams();
  q.set('page', options.page || 1);
  q.set('size', options.size || 20);
  if (options.provider) q.set('provider', options.provider);
  if (options.model) q.set('model', options.model);
  if (options.from) q.set('from', options.from);
  if (options.to) q.set('to', options.to);
  const url = `${getBaseURL()}/token-usage/me/records?${q.toString()}`;
  const res = await fetch(url, { credentials: 'include' });
  return await checkResponse(res);
}

// GET /api/v1/token-usage/me/records/{requestId}
async function getRecordDetail(userId, requestId) {
  if (!requestId) throw new Error('requestId is required');
  const url = `${getBaseURL()}/token-usage/me/records/${encodeURIComponent(requestId)}`;
  const res = await fetch(url, { credentials: 'include' });
  return await checkResponse(res);
}

// GET /api/v1/token-usage/me/status
async function getStatus(userId) {
  const url = `${getBaseURL()}/token-usage/me/status`;
  const res = await fetch(url, { credentials: 'include' });
  return await checkResponse(res);
}

// SSE-like stream using fetch + ReadableStream with the authenticated cookie.
// onEvent(parsedJson) will be called for each event data block.
// returns an object { close(): void }
async function connectStream(userId, onEvent, onError) {
  const url = `${getBaseURL()}/token-usage/me/stream`;
  const controller = new AbortController();
  const signal = controller.signal;

  try {
    const res = await fetch(url, { credentials: 'include', signal });
    if (!res.ok) {
      const body = await checkResponse(res); // will throw
      return null;
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    const pump = async () => {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        let index;
        while ((index = buffer.indexOf('\n\n')) !== -1) {
          const chunk = buffer.slice(0, index).trim();
          buffer = buffer.slice(index + 2);
          // parse SSE fields (very small parser)
          const lines = chunk.split(/\n/).map(l => l.replace(/^data:\s?/, ''));
          const data = lines.join('\n');
          if (!data) continue;
          try {
            const parsed = JSON.parse(data);
            onEvent && onEvent(parsed);
          } catch (e) {
            // non-json heartbeat or other text
            onEvent && onEvent({ raw: data });
          }
        }
      }
    };

    pump().catch(err => {
      if (err.name === 'AbortError') return;
      onError && onError(err);
    });

    return {
      close: () => controller.abort()
    };
  } catch (err) {
    onError && onError(err);
    return { close: () => controller.abort() };
  }
}

// Export for browser use
const tokenApi = {
  getBaseURL,
  postInternalEvent,
  getTodayStats,
  getSummary,
  getTrend,
  getRecords,
  getRecordDetail,
  getStatus,
  connectStream
};

// Attach to window for easy use in plain HTML/JS projects
if (typeof window !== 'undefined') window.tokenApi = tokenApi;

// CommonJS / module.exports for build environments (optional)
if (typeof module !== 'undefined' && module.exports) module.exports = tokenApi;
