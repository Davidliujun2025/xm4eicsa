export type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export type PageResponse<T> = {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
};

export type TokenQuotaItemResponse = {
  accountNo: string;
  accountName: string;
  dailyQuota: number;
  usedTokens: number;
  remainingTokens: number;
  overageTokens: number;
  usageRatePercent: number;
  aiCallCount: number;
  businessCount: number;
  statusCode: string;
  statusLabel: string;
  enabled: boolean;
};

export type TokenQuotaSummaryResponse = {
  totalAccounts: number;
  totalDailyQuota: number;
  totalUsedTokens: number;
  overallUsageRatePercent: number;
  highUsageCount: number;
  exceededCount: number;
};

export type UpdateDailyQuotaRequest = {
  dailyQuota: number;
  reason: string;
  operatorId: string;
  operatorName: string;
};

export type UpdateDailyQuotaResponse = {
  accountNo: string;
  beforeQuota: number;
  afterQuota: number;
  adjustedAt: string;
};

export type BatchUpdateDailyQuotaRequest = UpdateDailyQuotaRequest & {
  accountNos: string[];
};

export type BatchUpdateDailyQuotaResponse = {
  requestedCount: number;
  successCount: number;
  results: UpdateDailyQuotaResponse[];
};

export type AdjustmentLogResponse = {
  id: number;
  accountNo: string;
  accountName: string;
  beforeQuota: number;
  afterQuota: number;
  operatorId: string;
  operatorName: string;
  reason: string;
  adjustedAt: string;
};

export type ListAccountsParams = {
  accountNo?: string;
  statusCode?: string;
  page?: number;
  pageSize?: number;
};

export type ListLogsParams = {
  accountNo?: string;
  startTime?: string;
  endTime?: string;
  page?: number;
  pageSize?: number;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";
const TOKEN_QUOTA_API_PREFIX =
  import.meta.env.VITE_TOKEN_QUOTA_API_PREFIX ?? "/api/token-quota";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {})
    }
  });

  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok || !body.success) {
    throw new Error(body.message || "请求失败，请稍后重试");
  }

  return body.data;
}

function buildQuery(params: Record<string, string | number | undefined>) {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      searchParams.set(key, String(value));
    }
  });

  const query = searchParams.toString();
  return query ? `?${query}` : "";
}

export function listTokenQuotaAccounts(params: ListAccountsParams) {
  return request<PageResponse<TokenQuotaItemResponse>>(
    `${TOKEN_QUOTA_API_PREFIX}/accounts${buildQuery(params)}`
  );
}

export function getTokenQuotaSummary() {
  return request<TokenQuotaSummaryResponse>(`${TOKEN_QUOTA_API_PREFIX}/summary`);
}

export function updateDailyQuota(accountNo: string, payload: UpdateDailyQuotaRequest) {
  return request<UpdateDailyQuotaResponse>(
    `${TOKEN_QUOTA_API_PREFIX}/accounts/${encodeURIComponent(accountNo)}/daily-quota`,
    {
      method: "PUT",
      body: JSON.stringify(payload)
    }
  );
}

export function batchUpdateDailyQuota(payload: BatchUpdateDailyQuotaRequest) {
  return request<BatchUpdateDailyQuotaResponse>(
    `${TOKEN_QUOTA_API_PREFIX}/accounts/daily-quota/batch`,
    {
      method: "PUT",
      body: JSON.stringify(payload)
    }
  );
}

export function listAdjustmentLogs(params: ListLogsParams) {
  return request<PageResponse<AdjustmentLogResponse>>(
    `${TOKEN_QUOTA_API_PREFIX}/adjustment-logs${buildQuery(params)}`
  );
}
