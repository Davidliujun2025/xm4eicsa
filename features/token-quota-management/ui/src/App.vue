<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="logo">
        <div class="logo-icon">AI</div>
        <span>AI客服管理后台</span>
      </div>

      <nav class="menu">
        <div class="menu-item">工作台</div>
        <div class="menu-item">会话管理</div>
        <div class="menu-item">知识库管理</div>
        <div class="menu-item">客户管理</div>

        <div class="menu-group">数据统计</div>
        <div class="menu-item sub">Token使用统计</div>
        <div class="menu-item sub active">Token额度管理</div>
        <div class="menu-item sub">资源使用分析</div>

        <div class="menu-item" @click="openRecordModal">操作日志</div>
        <div class="menu-item">系统设置</div>
      </nav>

      <div class="user-card">
        <div class="avatar">张</div>
        <div>
          <div class="username">张三</div>
          <div class="role">超级管理员</div>
        </div>
      </div>

      <button class="logout">退出登录</button>
    </aside>

    <main class="main">
      <header class="page-header">
        <div>
          <h1>Token额度管理</h1>
          <p>管理所有客服账号的每日 Token 配额</p>
        </div>

        <button class="log-btn" @click="openRecordModal">操作日志</button>
      </header>

      <section class="stats-grid">
        <div class="stat-card">
          <div class="stat-title">客服账号总数</div>
          <div class="stat-value">
            {{ summary.totalAccounts }}
            <span>个</span>
          </div>
          <div class="stat-desc">当前系统账号数量</div>
        </div>

        <div class="stat-card">
          <div class="stat-title">每日配额总量</div>
          <div class="stat-value">
            {{ formatNumber(summary.totalDailyQuota) }}
            <span>Token</span>
          </div>
          <div class="stat-desc">全部客服每日配额</div>
        </div>

        <div class="stat-card">
          <div class="stat-title">今日已使用</div>
          <div class="stat-value">
            {{ formatNumber(summary.totalUsedTokens) }}
            <span>Token</span>
          </div>
          <div class="stat-desc">总体使用率 {{ summary.overallUsageRatePercent.toFixed(1) }}%</div>
          <div class="mini-progress">
            <span :style="{ width: Math.min(summary.overallUsageRatePercent, 100) + '%' }"></span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-title">使用率较高账号</div>
          <div class="stat-value">
            {{ summary.highUsageCount }}
            <span>个</span>
          </div>
          <div class="stat-desc danger">使用率 >= 80%</div>
        </div>

        <div class="stat-card">
          <div class="stat-title">超出建议额度账号</div>
          <div class="stat-value">
            {{ summary.exceededCount }}
            <span>个</span>
          </div>
          <div class="stat-desc danger">已超出每日配额</div>
        </div>
      </section>

      <section class="content-wrapper">
        <div class="table-panel">
          <div class="filter-bar">
            <div class="search-box">
              <input
                v-model="keyword"
                type="text"
                placeholder="请输入客服账号"
              />
              <span>🔍</span>
            </div>

            <select v-model="statusFilter">
              <option value="全部">状态：全部</option>
              <option value="NORMAL">正常</option>
              <option value="HIGH_USAGE">使用率较高</option>
              <option value="EXCEEDED_RECOMMENDED">超出建议额度</option>
            </select>

            <select v-model="rateFilter">
              <option value="全部">使用率：全部</option>
              <option value="NORMAL">低于80%</option>
              <option value="HIGH_USAGE">80%-100%</option>
              <option value="EXCEEDED_RECOMMENDED">达到或超过100%</option>
            </select>

            <button class="batch-btn" @click="openBatchEdit">批量调整额度</button>
          </div>

          <table class="quota-table">
            <thead>
              <tr>
                <th>
                  <input
                    type="checkbox"
                    :checked="isCurrentPageAllSelected"
                    @change="toggleCurrentPageSelected"
                  />
                </th>
                <th>客服账号</th>
                <th>每日配额(Token)</th>
                <th>已使用(Token)</th>
                <th>剩余(Token)</th>
                <th>使用率</th>
                <th>额度进度</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>

            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="empty-table">正在加载 Token 配额数据...</td>
              </tr>

              <tr v-else-if="tableData.length === 0">
                <td colspan="9" class="empty-table">
                  暂无匹配数据，请调整搜索或筛选条件
                </td>
              </tr>

              <template v-else>
                <tr v-for="item in tableData" :key="item.account">
                  <td>
                    <input
                      v-model="selectedAccounts"
                      type="checkbox"
                      :value="item.account"
                    />
                  </td>

                  <td>
                    <div class="account-cell">
                      <div class="table-avatar">{{ item.avatar }}</div>
                      <div>
                        <div class="name">{{ item.name }}</div>
                        <div class="account-id">{{ item.account }}</div>
                      </div>
                    </div>
                  </td>

                  <td>{{ formatNumber(item.quota) }}</td>
                  <td>{{ formatNumber(item.used) }}</td>
                  <td :class="{ danger: item.remaining < 0 }">
                    {{ formatNumber(item.remaining) }}
                  </td>

                  <td>
                    <span :class="['rate-text', item.statusType]">
                      {{ item.usageRate.toFixed(1) }}%
                    </span>
                  </td>

                  <td>
                    <div class="progress">
                      <span
                        :class="item.statusType + '-bg'"
                        :style="{ width: Math.min(item.usageRate, 100) + '%' }"
                      ></span>
                    </div>
                  </td>

                  <td>
                    <span :class="['status-tag', item.statusType + '-tag']">
                      {{ item.statusText }}
                    </span>
                  </td>

                  <td>
                    <button class="edit-btn" @click="openEditDrawer(item.account)">
                      修改额度
                    </button>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>

          <div class="pagination">
            <span>共 {{ pageInfo.total }} 条</span>

            <div class="page-list">
              <button @click="prevPage">&lt;</button>

              <button
                v-for="page in totalPage"
                :key="page"
                :class="{ 'page-active': currentPage === page }"
                @click="currentPage = page"
              >
                {{ page }}
              </button>

              <button @click="nextPage">&gt;</button>
            </div>

            <select v-model.number="pageSize">
              <option :value="5">5 条/页</option>
              <option :value="10">10 条/页</option>
              <option :value="20">20 条/页</option>
            </select>
          </div>
        </div>

        <aside class="drawer">
          <div class="drawer-header">
            <h3>{{ isBatchMode ? "批量修改额度" : "修改额度" }}</h3>
            <button type="button" @click="resetDrawer">×</button>
          </div>

          <template v-if="currentItem && !isBatchMode">
            <div class="drawer-user">
              <div class="drawer-avatar">{{ currentItem.avatar }}</div>
              <div>
                <div class="drawer-name">{{ currentItem.name }}</div>
                <div class="drawer-account">{{ currentItem.account }}</div>
              </div>
            </div>

            <div class="drawer-stats">
              <div>
                <p>当前每日配额</p>
                <strong>{{ formatNumber(currentItem.quota) }} Token</strong>
              </div>
              <div>
                <p>当前使用率</p>
                <strong class="danger-text">{{ currentItem.usageRate.toFixed(1) }}%</strong>
              </div>
              <div>
                <p>今日已使用</p>
                <strong>{{ formatNumber(currentItem.used) }} Token</strong>
              </div>
              <div>
                <p>剩余额度</p>
                <strong>{{ formatNumber(currentItem.remaining) }} Token</strong>
              </div>
            </div>
          </template>

          <template v-if="isBatchMode">
            <div class="batch-tip">
              已选择 <strong>{{ selectedAccounts.length }}</strong> 个客服账号，将统一调整每日 Token 配额。
            </div>
          </template>

          <div class="form-group">
            <label>调整每日配额</label>
            <div class="input-token">
              <input
                v-model.number="quotaInput"
                type="number"
                placeholder="请输入额度"
              />
              <span>Token</span>
            </div>
            <p class="help-text">系统允许范围：1 - 10,000,000 Token</p>
          </div>

          <div class="form-group">
            <label>调整原因</label>
            <textarea
              v-model="reasonInput"
              maxlength="200"
              placeholder="请输入调整原因..."
            ></textarea>
            <p class="word-count">{{ reasonInput.length }}/200</p>
          </div>

          <div class="drawer-actions">
            <button class="cancel-btn" type="button" @click="resetDrawer">取消</button>
            <button class="save-btn" type="button" :disabled="saving" @click="saveQuota">
              {{ saving ? "保存中..." : "保存" }}
            </button>
          </div>

          <div class="record-link">
            <span>额度调整记录</span>
            <a href="#" @click.prevent="openRecordModal">查看全部 &gt;</a>
          </div>
        </aside>
      </section>
    </main>

    <div v-if="recordModalVisible" class="modal-mask" @click.self="recordModalVisible = false">
      <div class="record-modal">
        <div class="modal-header">
          <h3>Token额度调整记录</h3>
          <button type="button" @click="recordModalVisible = false">×</button>
        </div>

        <table v-if="records.length > 0" class="record-table">
          <thead>
            <tr>
              <th>客服账号</th>
              <th>调整前</th>
              <th>调整后</th>
              <th>调整原因</th>
              <th>操作人</th>
              <th>调整时间</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="record in records" :key="record.id">
              <td>
                <div class="name">{{ record.name }}</div>
                <div class="account-id">{{ record.account }}</div>
              </td>
              <td>{{ formatNumber(record.oldQuota) }}</td>
              <td>{{ formatNumber(record.newQuota) }}</td>
              <td>{{ record.reason }}</td>
              <td>{{ record.operatorName }}</td>
              <td>{{ record.time }}</td>
            </tr>
          </tbody>
        </table>

        <div v-else class="empty-record">
          {{ recordsLoading ? "正在加载调整记录..." : "暂无额度调整记录" }}
        </div>
      </div>
    </div>

    <div v-if="toastVisible" class="toast" :class="toastType">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import {
  batchUpdateDailyQuota,
  getTokenQuotaSummary,
  listAdjustmentLogs,
  listTokenQuotaAccounts,
  updateDailyQuota,
  type AdjustmentLogResponse,
  type TokenQuotaItemResponse,
  type TokenQuotaSummaryResponse
} from "./api/tokenQuota";

type StatusType = "success" | "warning" | "danger";

type QuotaItem = {
  account: string;
  name: string;
  quota: number;
  used: number;
  remaining: number;
  usageRate: number;
  statusCode: string;
  statusText: string;
  statusType: StatusType;
  avatar: string;
};

type RecordItem = {
  id: number;
  name: string;
  account: string;
  oldQuota: number;
  newQuota: number;
  reason: string;
  operatorName: string;
  time: string;
};

const operatorId = "admin-001";
const operatorName = "张三";

const emptySummary: TokenQuotaSummaryResponse = {
  totalAccounts: 0,
  totalDailyQuota: 0,
  totalUsedTokens: 0,
  overallUsageRatePercent: 0,
  highUsageCount: 0,
  exceededCount: 0
};

const tableData = ref<QuotaItem[]>([]);
const summary = ref<TokenQuotaSummaryResponse>({ ...emptySummary });
const keyword = ref("");
const statusFilter = ref("全部");
const rateFilter = ref("全部");
const currentPage = ref(1);
const pageSize = ref(5);
const pageInfo = ref({ total: 0, totalPages: 1 });
const currentEditAccount = ref("");
const quotaInput = ref<number | null>(null);
const reasonInput = ref("");
const selectedAccounts = ref<string[]>([]);
const isBatchMode = ref(false);
const records = ref<RecordItem[]>([]);
const recordModalVisible = ref(false);
const loading = ref(false);
const saving = ref(false);
const recordsLoading = ref(false);
const toastVisible = ref(false);
const toastMessage = ref("");
const toastType = ref<"success" | "error" | "normal">("normal");

const currentItem = computed(() => {
  return tableData.value.find(item => item.account === currentEditAccount.value) || tableData.value[0];
});

const totalPage = computed(() => {
  return Math.max(pageInfo.value.totalPages, 1);
});

const isCurrentPageAllSelected = computed(() => {
  if (tableData.value.length === 0) return false;
  return tableData.value.every(item => selectedAccounts.value.includes(item.account));
});

watch([keyword, statusFilter, rateFilter, pageSize], () => {
  currentPage.value = 1;
  void loadAccounts();
});

watch(currentPage, () => {
  void loadAccounts();
});

onMounted(async () => {
  await Promise.all([loadAccounts(), loadSummary()]);
});

function statusCodeFromFilters() {
  if (statusFilter.value !== "全部") {
    return statusFilter.value;
  }
  if (rateFilter.value !== "全部") {
    return rateFilter.value;
  }
  return undefined;
}

async function loadAccounts() {
  loading.value = true;

  try {
    const data = await listTokenQuotaAccounts({
      accountNo: keyword.value.trim() || undefined,
      statusCode: statusCodeFromFilters(),
      page: currentPage.value,
      pageSize: pageSize.value
    });

    tableData.value = data.records.map(mapQuotaItem);
    pageInfo.value = {
      total: data.total,
      totalPages: Number(data.totalPages || 1)
    };

    selectedAccounts.value = selectedAccounts.value.filter(account =>
      tableData.value.some(item => item.account === account)
    );

    if (!currentEditAccount.value && tableData.value.length > 0) {
      currentEditAccount.value = tableData.value[0].account;
      quotaInput.value = tableData.value[0].quota;
    }
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  } finally {
    loading.value = false;
  }
}

async function loadSummary() {
  try {
    summary.value = await getTokenQuotaSummary();
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function loadRecords() {
  recordsLoading.value = true;

  try {
    const data = await listAdjustmentLogs({
      page: 1,
      pageSize: 50
    });
    records.value = data.records.map(mapRecordItem);
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  } finally {
    recordsLoading.value = false;
  }
}

function mapQuotaItem(item: TokenQuotaItemResponse): QuotaItem {
  return {
    account: item.accountNo,
    name: item.accountName,
    quota: item.dailyQuota,
    used: item.usedTokens,
    remaining: item.remainingTokens - item.overageTokens,
    usageRate: item.usageRatePercent,
    statusCode: item.statusCode,
    statusText: item.statusLabel,
    statusType: getStatusType(item.statusCode),
    avatar: item.accountName.slice(0, 1) || "客"
  };
}

function mapRecordItem(item: AdjustmentLogResponse): RecordItem {
  return {
    id: item.id,
    name: item.accountName,
    account: item.accountNo,
    oldQuota: item.beforeQuota,
    newQuota: item.afterQuota,
    reason: item.reason,
    operatorName: item.operatorName,
    time: formatDateTime(item.adjustedAt)
  };
}

function getStatusType(statusCode: string): StatusType {
  if (statusCode === "EXCEEDED_RECOMMENDED") return "danger";
  if (statusCode === "HIGH_USAGE") return "warning";
  return "success";
}

function formatNumber(num: number) {
  return Number(num).toLocaleString("zh-CN");
}

function formatDateTime(value: string) {
  return value ? value.replace("T", " ").slice(0, 19) : "-";
}

function openEditDrawer(account: string) {
  const item = tableData.value.find(row => row.account === account);

  if (!item) {
    showToast("未找到该客服账号", "error");
    return;
  }

  isBatchMode.value = false;
  currentEditAccount.value = account;
  quotaInput.value = item.quota;
  reasonInput.value = "";
}

function openBatchEdit() {
  if (selectedAccounts.value.length === 0) {
    showToast("请先勾选需要批量修改的客服账号", "error");
    return;
  }

  isBatchMode.value = true;
  quotaInput.value = null;
  reasonInput.value = "";
}

function resetDrawer() {
  isBatchMode.value = false;
  quotaInput.value = currentItem.value?.quota ?? null;
  reasonInput.value = "";
}

async function saveQuota() {
  const newQuota = Number(quotaInput.value);
  const reason = reasonInput.value.trim() || "管理员手动调整额度";

  if (!Number.isInteger(newQuota) || newQuota <= 0) {
    showToast("请输入正整数 Token 额度", "error");
    return;
  }

  if (newQuota > 10_000_000) {
    showToast("Token 额度不能超过 10,000,000", "error");
    return;
  }

  saving.value = true;

  try {
    if (isBatchMode.value) {
      await batchUpdateDailyQuota({
        accountNos: selectedAccounts.value,
        dailyQuota: newQuota,
        reason,
        operatorId,
        operatorName
      });
      selectedAccounts.value = [];
      isBatchMode.value = false;
      showToast("批量修改成功", "success");
    } else {
      if (!currentItem.value) {
        showToast("账号不存在，无法修改", "error");
        return;
      }
      await updateDailyQuota(currentItem.value.account, {
        dailyQuota: newQuota,
        reason,
        operatorId,
        operatorName
      });
      showToast("Token 额度修改成功", "success");
    }

    reasonInput.value = "";
    quotaInput.value = newQuota;
    await Promise.all([loadAccounts(), loadSummary(), recordModalVisible.value ? loadRecords() : Promise.resolve()]);
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  } finally {
    saving.value = false;
  }
}

function toggleCurrentPageSelected(event: Event) {
  const checked = (event.target as HTMLInputElement).checked;
  const accounts = tableData.value.map(item => item.account);

  if (checked) {
    accounts.forEach(account => {
      if (!selectedAccounts.value.includes(account)) {
        selectedAccounts.value.push(account);
      }
    });
  } else {
    selectedAccounts.value = selectedAccounts.value.filter(account => !accounts.includes(account));
  }
}

function prevPage() {
  if (currentPage.value <= 1) {
    showToast("已经是第一页了", "normal");
    return;
  }

  currentPage.value--;
}

function nextPage() {
  if (currentPage.value >= totalPage.value) {
    showToast("已经是最后一页了", "normal");
    return;
  }

  currentPage.value++;
}

async function openRecordModal() {
  recordModalVisible.value = true;
  await loadRecords();
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败，请稍后重试";
}

function showToast(message: string, type: "success" | "error" | "normal" = "normal") {
  toastMessage.value = message;
  toastType.value = type;
  toastVisible.value = true;

  window.setTimeout(() => {
    toastVisible.value = false;
  }, 1800);
}
</script>
