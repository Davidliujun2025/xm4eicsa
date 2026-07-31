<template>
  <section class="token-quota-page">
    <div class="card-row">
      <article class="stat-card">
        <strong>{{ summary.totalUsers }}</strong>
        <span>客服账号总数</span>
      </article>
      <article class="stat-card">
        <strong>{{ formatNumber(summary.totalQuota) }}</strong>
        <span>每日配额总量（Token）</span>
      </article>
      <article class="stat-card">
        <strong>{{ formatNumber(summary.usedToday) }}</strong>
        <span>今日已使用（Token）</span>
      </article>
      <article class="stat-card warning">
        <strong>{{ summary.highUsageCount }}</strong>
        <span>使用率达到 80% 的账号</span>
      </article>
      <article class="stat-card danger">
        <strong>{{ summary.exceededCount }}</strong>
        <span>已超出配额的账号</span>
      </article>
    </div>

    <div class="toolbar">
      <input
        v-model.trim="keyword"
        placeholder="搜索用户名或昵称"
        @keydown.enter="loadQuotas"
      />
      <button type="button" :disabled="loading" @click="loadQuotas">
        {{ loading ? '加载中…' : '查询' }}
      </button>
    </div>

    <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="message success">{{ successMessage }}</p>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>客服账号</th>
            <th>每日配额</th>
            <th>今日已使用</th>
            <th>剩余</th>
            <th>使用率</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.userId">
            <td class="user-cell">
              <span class="avatar">{{ avatarText(item) }}</span>
              <span>
                <b>{{ item.username }}</b>
                <small>{{ item.username }}</small>
              </span>
            </td>
            <td>{{ formatNumber(item.dailyTokenLimit) }}</td>
            <td>{{ formatNumber(item.usedToday) }}</td>
            <td :class="{ negative: item.remaining < 0 }">
              {{ formatNumber(item.remaining) }}
            </td>
            <td>
              <span :class="rateClass(item.usageRate)">
                {{ formatRate(item.usageRate) }}
              </span>
            </td>
            <td>{{ item.status === 'ENABLED' ? '启用' : '禁用' }}</td>
            <td>
              <button class="link-button" type="button" @click="startEdit(item)">调整配额</button>
            </td>
          </tr>
          <tr v-if="!loading && items.length === 0">
            <td colspan="7" class="empty">没有符合条件的客服账号</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="editingItem" class="dialog-mask" @click.self="cancelEdit">
      <form class="dialog" @submit.prevent="saveQuota">
        <h2>调整每日 Token 配额</h2>
        <p>{{ editingItem.username }}</p>
        <label>
          每日配额
          <input v-model.number="editingLimit" type="number" min="0" required />
        </label>
        <label>
          调整原因
          <textarea v-model.trim="editingReason" maxlength="200" placeholder="可选，最多 200 字"></textarea>
        </label>
        <div class="dialog-actions">
          <button type="button" class="secondary" @click="cancelEdit">取消</button>
          <button type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

type QuotaItem = {
  userId: number
  username: string
  status: string
  dailyTokenLimit: number
  usedToday: number
  remaining: number
  usageRate: number
}

type QuotaSummary = {
  totalUsers: number
  totalQuota: number
  usedToday: number
  highUsageCount: number
  exceededCount: number
}

const items = ref<QuotaItem[]>([])
const summary = ref<QuotaSummary>({
  totalUsers: 0,
  totalQuota: 0,
  usedToday: 0,
  highUsageCount: 0,
  exceededCount: 0,
})
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const editingItem = ref<QuotaItem | null>(null)
const editingLimit = ref(0)
const editingReason = ref('')

function getCookie(name: string) {
  return document.cookie
    .split('; ')
    .find((part) => part.startsWith(`${name}=`))
    ?.split('=')
    .slice(1)
    .join('=')
}

async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const csrf = getCookie('XSRF-TOKEN')
  const response = await fetch(path, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(csrf ? { 'X-XSRF-TOKEN': decodeURIComponent(csrf) } : {}),
      ...options.headers,
    },
  })
  if (response.status === 401) {
    window.location.href = `/?returnUrl=${encodeURIComponent(window.location.pathname)}`
    throw new Error('登录状态已失效')
  }
  if (response.status === 403) {
    throw new Error('当前账号没有 Token 配额管理权限')
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || '请求失败，请稍后重试')
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

async function loadQuotas() {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = new URLSearchParams()
    if (keyword.value) params.set('keyword', keyword.value)
    const result = await apiRequest<{ items: QuotaItem[]; summary: QuotaSummary }>(
      `/api/admin/token-quotas?${params.toString()}`,
    )
    items.value = result.items
    summary.value = result.summary
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function startEdit(item: QuotaItem) {
  editingItem.value = item
  editingLimit.value = item.dailyTokenLimit
  editingReason.value = ''
}

function cancelEdit() {
  editingItem.value = null
}

async function saveQuota() {
  if (!editingItem.value || editingLimit.value < 0) return
  saving.value = true
  errorMessage.value = ''
  try {
    await apiRequest<void>(`/api/admin/token-quotas/${editingItem.value.userId}`, {
      method: 'PATCH',
      body: JSON.stringify({
        dailyTokenLimit: editingLimit.value,
        reason: editingReason.value || null,
      }),
    })
    successMessage.value = 'Token 配额已更新'
    cancelEdit()
    await loadQuotas()
    window.setTimeout(() => {
      successMessage.value = ''
    }, 2000)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function formatNumber(value: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatRate(value: number) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function rateClass(value: number) {
  if (value >= 1) return 'rate danger-text'
  if (value >= 0.8) return 'rate warning-text'
  return 'rate success-text'
}

function avatarText(item: QuotaItem) {
  return (item.username || '?').slice(0, 1)
}

onMounted(loadQuotas)
</script>

<style scoped>
.token-quota-page { width: 100%; }
.card-row { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; min-height: 118px; flex-direction: column; justify-content: center; gap: 8px; padding: 20px; border: 1px solid #e5eaf2; border-radius: 14px; background: #fff; }
.stat-card strong { color: #102a56; font-size: 28px; }
.stat-card span { color: #667085; font-size: 13px; }
.stat-card.warning strong { color: #c47a08; }
.stat-card.danger strong, .negative, .danger-text { color: #d92d20; }
.warning-text { color: #c47a08; }
.success-text { color: #07883f; }
.toolbar { display: flex; justify-content: flex-end; gap: 10px; margin-bottom: 14px; }
.toolbar input, .dialog input, .dialog textarea { border: 1px solid #d8e0eb; border-radius: 9px; padding: 10px 12px; font: inherit; }
.toolbar input { width: 280px; }
button { border: 0; border-radius: 9px; background: #1769f6; color: white; padding: 10px 18px; cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: .55; }
.table-container { overflow: auto; border: 1px solid #e5eaf2; border-radius: 14px; background: #fff; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 14px 16px; border-bottom: 1px solid #edf1f6; text-align: left; white-space: nowrap; }
.data-table th { background: #f8fafc; color: #475467; font-size: 13px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-cell > span:last-child { display: flex; flex-direction: column; gap: 3px; }
.user-cell small { color: #98a2b3; }
.avatar { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; background: #eaf2ff; color: #1769f6; font-weight: 700; }
.rate { font-weight: 700; }
.link-button { padding: 0; background: none; color: #1769f6; }
.empty { color: #98a2b3; text-align: center !important; }
.message { margin: 0 0 12px; padding: 10px 14px; border-radius: 8px; }
.message.error { background: #fff1f0; color: #d92d20; }
.message.success { background: #ecfdf3; color: #07883f; }
.dialog-mask { position: fixed; inset: 0; z-index: 30; display: grid; place-items: center; padding: 20px; background: rgb(15 23 42 / 45%); }
.dialog { width: min(440px, 100%); padding: 24px; border-radius: 16px; background: #fff; box-shadow: 0 24px 80px rgb(15 23 42 / 24%); }
.dialog h2 { margin: 0 0 8px; color: #102a56; }
.dialog p { margin: 0 0 20px; color: #667085; }
.dialog label { display: grid; gap: 7px; margin-top: 14px; color: #344054; font-weight: 600; }
.dialog textarea { min-height: 82px; resize: vertical; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
.dialog-actions .secondary { background: #eef2f6; color: #344054; }
@media (max-width: 1100px) { .card-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
