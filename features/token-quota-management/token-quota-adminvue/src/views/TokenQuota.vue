<template>
  <div class="token-quota-page">
    <div class="page-top">
      <div class="page-title-wrap">
        <h1 class="page-title">Token额度管理</h1>
        <p class="page-desc">管理所有客服账号的每日 Token 配额</p>
      </div>
      <button class="btn-primary">操作日志</button>
    </div>

    <!-- 统计卡片区域 -->
    <div class="card-row">
      <div class="stat-card">
        <div class="stat-value">12 <span class="unit">个</span></div>
        <div class="stat-label">客服账号总数</div>
        <div class="stat-tip">当前系统账号数量</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">116,000 <span class="unit">Token</span></div>
        <div class="stat-label">每日配额总量</div>
        <div class="stat-tip">全部客服每日配额</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">92,160 <span class="unit">Token</span></div>
        <div class="stat-label">今日已使用</div>
        <div class="stat-tip">总体使用率 79.4%</div>
        <div class="progress-bar">
          <div class="progress-inner" style="width:79.4%"></div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-value">5 <span class="unit">个</span></div>
        <div class="stat-label">使用率较高账号</div>
        <div class="stat-tip warn-text">使用率 ≥ 80%</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">2 <span class="unit">个</span></div>
        <div class="stat-label">超出建议额度账号</div>
        <div class="stat-tip danger-text">已超出每日配额</div>
      </div>
    </div>

    <!-- 主体区域：上下布局【筛选栏 + 表格】，右侧抽屉悬浮 -->
    <div class="main-container">
      <div class="table-container">
        <!-- 筛选工具栏：表格正上方 -->
        <div class="table-toolbar">
          <div class="search-input">
            <input placeholder="请输入客服账号" />
            <span>🔍</span>
          </div>
          <div class="select-group">
            <select>
              <option>状态：全部</option>
            </select>
            <select>
              <option>使用率：全部</option>
            </select>
            <button class="btn-primary">批量调整额度</button>
          </div>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th><input type="checkbox"></th>
              <th>客服账号</th>
              <th>每日配额(Token)</th>
              <th>已使用(Token)</th>
              <th>剩余(Token)</th>
              <th>使用率</th>
              <th>额度进度</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tableData" :key="item.id">
              <td><input type="checkbox"></td>
              <td class="user-cell">
                <span class="avatar">{{ item.name.slice(0,1) }}</span>
                <div>
                  <div>{{ item.name }}</div>
                  <div class="sub-text">{{ item.account }}</div>
                </div>
              </td>
              <td>{{ formatNumber(item.quota) }}</td>
              <td>{{ formatNumber(item.used) }}</td>
              <td :class="isRemainNegative(item) ? 'danger-text' : ''">
                {{ formatNumber(getRemain(item)) }}
              </td>
              <td :class="getRateClass(item)">{{ getRate(item) }}%</td>
              <td>
                <div class="progress-bar-small">
                  <div class="progress-inner" :style="{width:getRate(item)+'%'}"></div>
                </div>
              </td>
              <td></td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 右侧修改额度抽屉 -->
      <div class="drawer-right">
        <div class="drawer-header">
          <h3>修改额度</h3>
          <span>×</span>
        </div>
        <div class="drawer-user">
          <span class="avatar">李</span>
          <div>李明<br>客服001</div>
        </div>
        <div class="form-row">
          <div class="form-col">
            <div class="label">当前每日配额</div>
            <div class="value">10,000 Token</div>
          </div>
          <div class="form-col">
            <div class="label">当前使用率</div>
            <div class="value warn-text">85.6%</div>
          </div>
        </div>
        <div class="form-row">
          <div class="form-col">
            <div class="label">今日已使用</div>
            <div class="value">8,560 Token</div>
          </div>
          <div class="form-col">
            <div class="label">剩余额度</div>
            <div class="value">1,440 Token</div>
          </div>
        </div>
        <div class="form-item">
          <label>调整每日配额</label>
          <div class="input-wrap">
            <input value="10000"/>
            <span>Token</span>
          </div>
          <div class="tip">建议额度范围：1,000 - 50,000 Token</div>
        </div>
        <div class="form-item">
          <label>调整原因（选填）</label>
          <textarea placeholder="请输入调整原因..."></textarea>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
type QuotaItem = {
  id: number
  name: string
  account: string
  quota: number
  used: number
}

const tableData: QuotaItem[] = [
  { id:1, name:'李明', account:'客服001', quota:10000, used:8560 },
  { id:2, name:'王芳', account:'客服002', quota:8000, used:5120 },
  { id:3, name:'张伟', account:'客服003', quota:15000, used:13200 },
  { id:4, name:'刘洋', account:'客服004', quota:12000, used:12500 },
  { id:5, name:'陈晨', account:'客服005', quota:6000, used:2340 },
]

function formatNumber(num: number) {
  return Number(num).toLocaleString("zh-CN")
}
function getRemain(item: QuotaItem) {
  return item.quota - item.used
}
function isRemainNegative(item: QuotaItem) {
  return getRemain(item) < 0
}
function getRate(item: QuotaItem) {
  if(item.quota === 0) return 0
  return Number(((item.used / item.quota) * 100).toFixed(1))
}
function getRateClass(item: QuotaItem) {
  const rate = getRate(item)
  if(rate >= 100) return 'danger-text'
  if(rate >= 80) return 'warn-text'
  return 'success-text'
}
</script>

<style scoped>
.token-quota-page {
  width: 100%;
}
.page-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.page-title {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 6px;
}
.page-desc {
  color: var(--text-secondary);
}
.btn-primary {
  background-color: var(--color-primary);
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  font-size:15px;
}
.btn-primary:hover {
  background-color: var(--color-primary-hover);
}

.card-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap:16px;
  margin-bottom:24px;
}
.stat-card {
  background:#fff;
  border-radius:12px;
  padding:20px;
  border:1px solid var(--border-color);
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom:8px;
}
.unit {
  font-size:14px;
  font-weight:normal;
  color:var(--text-secondary);
}
.stat-label {
  color:#666;
  font-size:14px;
  margin-bottom:4px;
}
.stat-tip {
  font-size:13px;
}
.progress-bar {
  width:100%;
  height:6px;
  background:#e5e7eb;
  border-radius:99px;
  margin-top:10px;
}
.progress-inner {
  height:100%;
  background:var(--color-primary);
  border-radius:99px;
}

/* 主体：表格区域 + 右侧抽屉 */
.main-container {
  display:flex;
  gap:16px;
}
.table-container {
  flex:1;
  display:flex;
  flex-direction:column;
}
.table-toolbar {
  display:flex;
  justify-content: space-between;
  align-items:center;
  margin-bottom:16px;
}
.search-input {
  border:1px solid var(--border-color);
  border-radius:8px;
  padding:8px 12px;
  display:flex;
  align-items:center;
  gap:6px;
}
.search-input input {
  border:none;
  outline:none;
}
.select-group {
  display:flex;
  gap:12px;
  align-items:center;
}
select {
  border:1px solid var(--border-color);
  border-radius:8px;
  padding:8px 12px;
  background:#fff;
}

.data-table {
  border-collapse: collapse;
  width:100%;
  background:#fff;
  border-radius:12px;
  overflow:hidden;
  border:1px solid var(--border-color);
}
.data-table th, .data-table td {
  padding:14px 16px;
  text-align:left;
  border-bottom:1px solid var(--border-color);
  font-size:14px;
}
.data-table th {
  background:#f9fafb;
}
.user-cell {
  display:flex;
  align-items:center;
  gap:10px;
}
.avatar {
  width:34px;
  height:34px;
  border-radius:50%;
  background:var(--color-primary);
  color:#fff;
  display:flex;
  align-items:center;
  justify-content:center;
  flex-shrink:0;
}
.sub-text {
  font-size:12px;
  color:#888;
}
.progress-bar-small {
  width:120px;
  height:6px;
  background:#eee;
  border-radius:99px;
}

.drawer-right {
  width:360px;
  flex-shrink:0;
  background:#fff;
  border:1px solid var(--border-color);
  border-radius:12px;
  padding:20px;
}
.drawer-header {
  display:flex;
  justify-content:space-between;
  align-items:center;
  margin-bottom:20px;
}
.drawer-user {
  display:flex;
  align-items:center;
  gap:10px;
  margin-bottom:24px;
}
.form-row {
  display:grid;
  grid-template-columns: 1fr 1fr;
  gap:16px;
  margin-bottom:20px;
}
.form-item {
  margin-bottom:18px;
}
.form-item label {
  display:block;
  margin-bottom:8px;
  font-size:14px;
}
.input-wrap {
  border:1px solid var(--border-color);
  border-radius:8px;
  display:flex;
  align-items:center;
}
.input-wrap input {
  flex:1;
  border:none;
  padding:10px 12px;
  outline:none;
}
textarea {
  width:100%;
  border:1px solid var(--border-color);
  border-radius:8px;
  padding:10px 12px;
  min-height:80px;
  outline:none;
}
.tip {
  font-size:12px;
  color:#888;
  margin-top:4px;
}
</style>