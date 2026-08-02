const PAGE_PARAMS = new URLSearchParams(window.location.search);
const API_BASE =
  PAGE_PARAMS.get("apiBase") ||
  window.localStorage.getItem("forbiddenWordsApiBase") ||
  `${window.location.origin}/api`;
const PAGE_SIZE = 20;
const state = {
  currentPlatform: "ALL",
  currentPage: 1,
  operationPage: 1,
  hitPage: 1,
  wordRows: [],
  csvPreviewRows: [],
  csvRawContent: "",
  csvFileName: ""
};

const platforms = [
  "ALL",
  "TAOBAO",
  "TMALL",
  "JD",
  "PINDUODUO",
  "DOUYIN",
  "XIAOHONGSHU",
  "KUAISHOU",
  "SHIPINHAO",
  "WECHAT_SHOP",
  "OTHER"
];
const platformLabels = {
  ALL: "全部平台",
  TAOBAO: "淘宝",
  TMALL: "天猫",
  JD: "京东",
  PINDUODUO: "拼多多",
  DOUYIN: "抖音",
  XIAOHONGSHU: "小红书",
  KUAISHOU: "快手",
  SHIPINHAO: "视频号",
  WECHAT_SHOP: "微信小店",
  OTHER: "其他平台"
};

const SUPPORTED_PLATFORM_COUNT = platforms.filter((p) => p !== "ALL").length;

function qs(id) {
  return document.getElementById(id);
}

function createPlatformOptions(el, withAll = true) {
  const list = withAll ? platforms : platforms.filter((p) => p !== "ALL");
  el.innerHTML = list
    .map((p) => `<option value="${p}">${platformLabels[p] || p}</option>`)
    .join("");
}

function displayPlatform(platform) {
  return platformLabels[platform] || platform || "-";
}

function formatTime(text) {
  if (!text) return "-";
  return new Date(text).toLocaleString("zh-CN", { hour12: false });
}

async function api(path, options = {}) {
  const csrfCookie = document.cookie
    .split("; ")
    .find((item) => item.startsWith("XSRF-TOKEN="));
  const csrfToken = csrfCookie
    ? decodeURIComponent(csrfCookie.split("=").slice(1).join("="))
    : "";
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {}),
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    const txt = await response.text();
    throw new Error(txt || "请求失败");
  }
  return response.json();
}

function renderStats(totalWords, totalTriggerCount) {
  const words = Number.isFinite(totalWords) ? totalWords.toLocaleString("zh-CN") : "-";
  const triggerCount = Number.isFinite(totalTriggerCount) ? totalTriggerCount.toLocaleString("zh-CN") : "-";
  qs("stats").innerHTML = `
    <div class="card"><h5>总违禁词数</h5><strong>${words}</strong></div>
    <div class="card"><h5>覆盖平台数</h5><strong>${SUPPORTED_PLATFORM_COUNT}</strong></div>
    <div class="card"><h5>今日触发次数</h5><strong>${triggerCount}</strong></div>
  `;
}

function actionTag(action) {
  if (action === "ADD") return '<span class="tag tag-success">添加</span>';
  if (action === "DELETE") return '<span class="tag tag-danger">移除</span>';
  if (action === "BATCH_IMPORT") return '<span class="tag tag-info">批量导入</span>';
  return `<span class="tag">${action || "-"}</span>`;
}

function sourceTag(sourceType) {
  if (sourceType === "AI_ANSWER" || sourceType === "AI") return "AI 回复";
  if (sourceType === "USER_QUESTION" || sourceType === "USER") return "用户问题";
  return sourceType || "-";
}

function buildConversationId(row) {
  const dt = row.actionTime ? new Date(row.actionTime) : new Date();
  const y = dt.getFullYear();
  const m = String(dt.getMonth() + 1).padStart(2, "0");
  const d = String(dt.getDate()).padStart(2, "0");
  return `CONV-${y}${m}${d}-${String(row.id || 0).padStart(3, "0")}`;
}

function buildPager(targetId, page, total, onChange) {
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  qs(targetId).innerHTML = `
    <span>共 ${total} 条，第 ${page}/${totalPages} 页</span>
    <button ${page <= 1 ? "disabled" : ""} data-action="prev">上一页</button>
    <button ${page >= totalPages ? "disabled" : ""} data-action="next">下一页</button>
  `;

  qs(targetId).querySelectorAll("button").forEach((btn) => {
    btn.addEventListener("click", () => {
      const nextPage = btn.dataset.action === "prev" ? page - 1 : page + 1;
      onChange(nextPage);
    });
  });
}

async function loadWords() {
  const searchText = qs("searchInput").value.trim().toLowerCase();
  const data = await api(`/forbidden-words?platform=${state.currentPlatform}&page=${state.currentPage}&size=${PAGE_SIZE}`);
  state.wordRows = data.items.filter((row) => row.word.toLowerCase().includes(searchText));

  const tbody = qs("wordTableBody");
  tbody.innerHTML = state.wordRows.map((row) => `
    <tr>
      <td>${row.word}</td>
      <td><span class="badge">${displayPlatform(row.platform)}</span></td>
      <td>${formatTime(row.createdAt)}</td>
      <td>${row.createdBy || "-"}</td>
      <td><button class="text-danger" data-id="${row.id}">移除</button></td>
    </tr>
  `).join("");

  tbody.querySelectorAll("button").forEach((btn) => {
    btn.addEventListener("click", async () => {
      if (!confirm("确认移除该违禁词吗？此操作将物理删除。")) {
        return;
      }
      await api(`/forbidden-words/${btn.dataset.id}`, { method: "DELETE" });
      await loadWords();
      await loadOperationLogs();
    });
  });

  buildPager("wordPager", data.page, data.total, async (nextPage) => {
    state.currentPage = nextPage;
    await loadWords();
  });

  const hits = await api(`/chat-audit/logs?page=1&size=1`);
  renderStats(data.total, hits.total);
}

async function loadOperationLogs() {
  const operator = encodeURIComponent(qs("operatorFilter").value.trim());
  const action = encodeURIComponent(qs("actionFilter").value.trim());
  const platform = encodeURIComponent(qs("operationPlatformFilter").value.trim());
  const keyword = qs("opKeyword").value.trim().toLowerCase();
  const startDate = qs("opDateStart").value;
  const endDate = qs("opDateEnd").value;
  const data = await api(`/forbidden-words/audit/operations?operator=${operator}&action=${action}&page=${state.operationPage}&size=${PAGE_SIZE}`);

  const rows = data.items.filter((row) => {
    const p = (row.platform || "").toString();
    const t = (row.targetWord || "").toLowerCase();
    const hitKeyword = !keyword || t.includes(keyword);
    const hitPlatform = !platform || platform === "ALL" || p === platform;

    if (!startDate && !endDate) return hitKeyword && hitPlatform;
    const ts = row.operationTime ? new Date(row.operationTime).getTime() : 0;
    const startTs = startDate ? new Date(`${startDate}T00:00:00`).getTime() : 0;
    const endTs = endDate ? new Date(`${endDate}T23:59:59`).getTime() : Number.MAX_SAFE_INTEGER;
    return hitKeyword && hitPlatform && ts >= startTs && ts <= endTs;
  });

  qs("operationBody").innerHTML = rows.map((row) => `
    <tr>
      <td>${formatTime(row.operationTime)}</td>
      <td>${row.operator}</td>
      <td>${row.operatorIp}</td>
      <td>${actionTag(row.action)}</td>
      <td>${displayPlatform(row.platform)}</td>
      <td>"${row.targetWord || "-"}"</td>
    </tr>
  `).join("");

  buildPager("operationPager", data.page, data.total, async (nextPage) => {
    state.operationPage = nextPage;
    await loadOperationLogs();
  });
}

async function loadHitLogs() {
  const actor = encodeURIComponent(qs("actorFilter").value.trim());
  const platform = qs("hitPlatformFilter").value;
  const keyword = qs("hitKeyword").value.trim().toLowerCase();
  const startDate = qs("hitDateStart").value;
  const endDate = qs("hitDateEnd").value;
  const data = await api(`/chat-audit/logs?actor=${actor}&platform=${platform}&page=${state.hitPage}&size=${PAGE_SIZE}`);

  const actorDayCount = {};
  data.items.forEach((row) => {
    const day = row.actionTime ? row.actionTime.slice(0, 10) : "";
    const key = `${row.actor || ""}_${day}`;
    actorDayCount[key] = (actorDayCount[key] || 0) + 1;
  });

  const rows = data.items.filter((row) => {
    const dateValue = row.actionTime ? new Date(row.actionTime).getTime() : 0;
    const startTs = startDate ? new Date(`${startDate}T00:00:00`).getTime() : 0;
    const endTs = endDate ? new Date(`${endDate}T23:59:59`).getTime() : Number.MAX_SAFE_INTEGER;
    const passDate = dateValue >= startTs && dateValue <= endTs;
    const passKeyword =
      !keyword ||
      (row.hitWord || "").toLowerCase().includes(keyword) ||
      buildConversationId(row).toLowerCase().includes(keyword);
    return passDate && passKeyword;
  });

  qs("hitBody").innerHTML = rows.map((row) => {
    const day = row.actionTime ? row.actionTime.slice(0, 10) : "";
    const actorKey = `${row.actor || ""}_${day}`;
    const count = actorDayCount[actorKey] || 0;
    const remindTag = count >= 3 ? `<span class="tag tag-warn">今日 ${count}次</span>` : "";
    return `
    <tr>
      <td>${formatTime(row.actionTime)}</td>
      <td>${row.actor} ${remindTag}</td>
      <td>${displayPlatform(row.platform)}</td>
      <td><span class="hit-word">${row.hitWord}</span></td>
      <td>${sourceTag(row.sourceType)}</td>
      <td><a href="#" class="conv-id">${buildConversationId(row)}</a></td>
      <td><span class="action-remind">提醒客服</span></td>
    </tr>
  `;
  }).join("");

  buildPager("hitPager", data.page, data.total, async (nextPage) => {
    state.hitPage = nextPage;
    await loadHitLogs();
  });
}

function renderCsvPreview(rows) {
  const html = `
    <table>
      <thead><tr><th>行号</th><th>词条</th><th>平台</th><th>状态</th><th>错误</th></tr></thead>
      <tbody>
        ${rows.map((r) => `
          <tr class="${r.valid ? "" : "invalid"}">
            <td>${r.rowNumber}</td>
            <td>${r.word || "-"}</td>
            <td>${displayPlatform(r.platform)}</td>
            <td>${r.valid ? "通过" : "错误"}</td>
            <td>${r.error || "-"}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
  qs("csvPreview").innerHTML = html;
}

function csvCell(value) {
  const text = (value ?? "").toString();
  if (/[",\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

function downloadCsv(filename, headers, rows) {
  const lines = [headers.map(csvCell).join(",")]
    .concat(rows.map((r) => r.map(csvCell).join(",")));
  const blob = new Blob([`\uFEFF${lines.join("\n")}`], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function formatExportTime() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${y}${m}${day}-${hh}${mm}${ss}`;
}

function buildOperationExportRows() {
  return Array.from(qs("operationBody").querySelectorAll("tr")).map((tr) => {
    const tds = tr.querySelectorAll("td");
    return [
      tds[0]?.textContent?.trim() || "",
      tds[1]?.textContent?.trim() || "",
      tds[2]?.textContent?.trim() || "",
      tds[3]?.textContent?.trim() || "",
      tds[4]?.textContent?.trim() || "",
      tds[5]?.textContent?.trim() || ""
    ];
  });
}

function buildHitExportRows() {
  return Array.from(qs("hitBody").querySelectorAll("tr")).map((tr) => {
    const tds = tr.querySelectorAll("td");
    return [
      tds[0]?.textContent?.trim() || "",
      tds[1]?.textContent?.trim() || "",
      tds[2]?.textContent?.trim() || "",
      tds[3]?.textContent?.trim() || "",
      tds[4]?.textContent?.trim() || "",
      tds[5]?.textContent?.trim() || "",
      tds[6]?.textContent?.trim() || ""
    ];
  });
}

function updateCsvFileMeta() {
  const meta = qs("csvFileMeta");
  if (!state.csvFileName) {
    meta.textContent = "尚未选择文件";
    return;
  }
  meta.textContent = `已选择：${state.csvFileName}`;
}

function resetCsvState() {
  state.csvRawContent = "";
  state.csvFileName = "";
  state.csvPreviewRows = [];
  const input = qs("csvFileInput");
  input.value = "";
  qs("csvPreview").innerHTML = "";
  updateCsvFileMeta();
}

function readCsvFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("文件读取失败"));
    reader.readAsText(file, "utf-8");
  });
}

async function handleCsvFile(file) {
  if (!file) return;
  const isCsv = file.name.toLowerCase().endsWith(".csv") || file.type === "text/csv";
  if (!isCsv) {
    alert("请上传 .csv 文件");
    return;
  }
  state.csvRawContent = await readCsvFile(file);
  state.csvFileName = file.name;
  state.csvPreviewRows = [];
  qs("csvPreview").innerHTML = "";
  updateCsvFileMeta();
}

function bindTabs() {
  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", async () => {
      document.querySelectorAll(".tab").forEach((el) => el.classList.remove("active"));
      document.querySelectorAll(".tab-content").forEach((el) => el.classList.remove("active"));
      tab.classList.add("active");
      qs(tab.dataset.tab).classList.add("active");

      if (tab.dataset.tab === "operation-log") {
        await loadOperationLogs();
      }
      if (tab.dataset.tab === "hit-log") {
        await loadHitLogs();
      }
    });
  });
}

function bindDialogs() {
  const addDialog = qs("addDialog");
  qs("addOpenBtn").addEventListener("click", () => addDialog.showModal());
  qs("cancelAdd").addEventListener("click", () => addDialog.close());

  qs("confirmAdd").addEventListener("click", async () => {
    const payload = {
      word: qs("newWord").value.trim(),
      platform: qs("newWordPlatform").value
    };
    if (!payload.word) {
      alert("请输入违禁词");
      return;
    }
    const start = Date.now();
    await api("/forbidden-words", { method: "POST", body: JSON.stringify(payload) });
    const duration = Date.now() - start;
    if (duration > 2000) {
      alert(`警告：新增耗时 ${duration}ms，超过 2 秒验收目标`);
    }
    addDialog.close();
    qs("newWord").value = "";
    await loadWords();
    await loadOperationLogs();
  });

  const csvDialog = qs("csvDialog");
  const csvDropzone = qs("csvDropzone");
  const csvFileInput = qs("csvFileInput");
  const chooseCsvBtn = qs("chooseCsvBtn");

  qs("uploadCsvBtn").addEventListener("click", () => {
    resetCsvState();
    csvDialog.showModal();
  });
  qs("cancelCsv").addEventListener("click", () => {
    csvDialog.close();
    resetCsvState();
  });

  chooseCsvBtn.addEventListener("click", () => {
    csvFileInput.click();
  });

  csvDropzone.addEventListener("click", () => {
    csvFileInput.click();
  });

  csvDropzone.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      csvFileInput.click();
    }
  });

  csvFileInput.addEventListener("change", async (e) => {
    const [file] = e.target.files || [];
    try {
      await handleCsvFile(file);
    } catch (err) {
      alert(err.message || "读取文件失败");
    }
  });

  csvDropzone.addEventListener("dragover", (e) => {
    e.preventDefault();
    csvDropzone.classList.add("dragover");
  });

  csvDropzone.addEventListener("dragleave", () => {
    csvDropzone.classList.remove("dragover");
  });

  csvDropzone.addEventListener("drop", async (e) => {
    e.preventDefault();
    csvDropzone.classList.remove("dragover");
    const [file] = e.dataTransfer?.files || [];
    try {
      await handleCsvFile(file);
    } catch (err) {
      alert(err.message || "读取文件失败");
    }
  });

  qs("previewCsvBtn").addEventListener("click", async () => {
    if (!state.csvRawContent.trim()) {
      alert("请先选择 CSV 文件");
      return;
    }
    const csvContent = state.csvRawContent;
    const rows = await api("/forbidden-words/csv/preview", {
      method: "POST",
      body: JSON.stringify({ csvContent })
    });
    state.csvPreviewRows = rows;
    renderCsvPreview(rows);
  });

  qs("confirmCsvBtn").addEventListener("click", async () => {
    if (!state.csvRawContent.trim()) {
      alert("请先选择 CSV 文件");
      return;
    }
    if (!state.csvPreviewRows.length) {
      alert("请先点击预览并确认数据");
      return;
    }
    const hasError = state.csvPreviewRows.some((r) => !r.valid);
    if (hasError) {
      alert("存在格式错误行，请修复后再确认导入");
      return;
    }
    await api("/forbidden-words/csv/confirm", {
      method: "POST",
      body: JSON.stringify({ previewItems: state.csvPreviewRows })
    });
    csvDialog.close();
    resetCsvState();
    await loadWords();
    await loadOperationLogs();
  });
}

function bindFilters() {
  qs("platformSelect").addEventListener("change", async (e) => {
    state.currentPlatform = e.target.value;
    state.currentPage = 1;
    await loadWords();
  });

  qs("searchInput").addEventListener("input", async () => {
    await loadWords();
  });

  qs("reloadOps").addEventListener("click", async () => {
    state.operationPage = 1;
    await loadOperationLogs();
  });

  qs("exportOps").addEventListener("click", () => {
    const rows = buildOperationExportRows();
    if (!rows.length) {
      alert("暂无可导出的变更历史数据");
      return;
    }
    downloadCsv(
      `变更历史-${formatExportTime()}.csv`,
      ["操作时间", "操作人", "IP 地址", "操作类型", "生效平台", "违禁词"],
      rows
    );
  });

  qs("reloadHits").addEventListener("click", async () => {
    state.hitPage = 1;
    await loadHitLogs();
  });

  qs("exportHits").addEventListener("click", () => {
    const rows = buildHitExportRows();
    if (!rows.length) {
      alert("暂无可导出的触发记录数据");
      return;
    }
    downloadCsv(
      `触发记录-${formatExportTime()}.csv`,
      ["触发时间", "客服人员", "所属平台", "触发违禁词", "触发场景", "对话 ID", "操作"],
      rows
    );
  });
}

function bindSidebarNavigation() {
  const isLocalDevelopment =
    window.location.hostname === "127.0.0.1" || window.location.hostname === "localhost";
  const localUrl = (port) => `${window.location.protocol}//${window.location.hostname}:${port}/`;
  const navMap = {
    "用户管理": PAGE_PARAMS.get("usersUrl") || (isLocalDevelopment ? localUrl(5176) : `${window.location.origin}/admin/users/`),
    "Token 管理": PAGE_PARAMS.get("tokensUrl") || (isLocalDevelopment ? localUrl(5177) : `${window.location.origin}/admin/tokens/`),
    "违禁词管理": PAGE_PARAMS.get("forbiddenUrl") || (isLocalDevelopment ? localUrl(5179) : `${window.location.origin}/admin/forbidden-words/`)
  };

  document.querySelectorAll(".nav-menu .nav-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      const label = btn.querySelector("span")?.textContent?.trim();
      const target = label ? navMap[label] : "";
      if (target) {
        window.location.href = target;
      }
    });
  });
}

async function loadCurrentUser() {
  const response = await fetch("/api/v1/auth/me", { credentials: "include" });
  if (response.status === 401) {
    window.location.href = `/?returnUrl=${encodeURIComponent(window.location.pathname)}`;
    throw new Error("登录状态已失效");
  }
  if (!response.ok) {
    throw new Error("无法获取当前登录用户");
  }
  const payload = await response.json();
  const user = payload.data || {};
  const displayName = user.username || "";
  qs("currentUserName").textContent = displayName;
  qs("currentUserAvatar").textContent = displayName.slice(0, 1) || "?";
}

async function bootstrap() {
  renderStats();
  createPlatformOptions(qs("platformSelect"));
  createPlatformOptions(qs("newWordPlatform"), false);
  createPlatformOptions(qs("hitPlatformFilter"));
  createPlatformOptions(qs("operationPlatformFilter"));

  bindTabs();
  bindDialogs();
  bindFilters();
  bindSidebarNavigation();

  await loadCurrentUser();
  await loadWords();
}

bootstrap().catch((err) => {
  console.error(err);
  alert(`初始化失败：${err.message}`);
});
