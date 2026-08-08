import type {
  ApiErrorResponse,
  CreateAgentRequest,
  CreateAgentResponse,
  OperationLogResponse,
  ResetPasswordRequest,
  ResetPasswordResponse,
  UpdateAgentRequest,
  UpdateAgentResponse,
  UpdateStatusRequest,
  UpdateStatusResponse,
  UserListResponse,
  UserRole,
  UserStatus,
} from "../types/user";

function resolveApiBaseUrl(): string {
  // Local development uses the Vite proxy and production uses Nginx. Both
  // must call /api on the current origin, never a baked 127.0.0.1 address.
  return "";
}

const API_BASE_URL = resolveApiBaseUrl();

function getCookie(name: string): string | undefined {
  return document.cookie
    .split("; ")
    .find((item) => item.startsWith(`${name}=`))
    ?.split("=")
    .slice(1)
    .join("=");
}

export class ApiError extends Error {
  status: number;
  response: ApiErrorResponse;

  constructor(
    status: number,
    response: ApiErrorResponse,
  ) {
    super(response.message);

    this.name = "ApiError";
    this.status = status;
    this.response = response;
  }
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  return Boolean(
    value &&
    typeof value === "object" &&
    "code" in value &&
    "message" in value,
  );
}

async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  let response: Response;

  try {
    response = await fetch(
      `${API_BASE_URL}${path}`,
      {
        ...options,
        credentials: "include",
        headers: {
          "Content-Type":
            "application/json",
          ...(getCookie("XSRF-TOKEN")
            ? { "X-XSRF-TOKEN": decodeURIComponent(getCookie("XSRF-TOKEN")!) }
            : {}),
          ...options.headers,
        },
      },
    );
  } catch {
    throw new Error(
      "无法连接后端，请确认 Spring Boot 已启动",
    );
  }

  const responseText = await response.text();
  let result: unknown = null;

  if (responseText) {
    try {
      result = JSON.parse(responseText);
    } catch {
      const contentType =
        response.headers.get("content-type") ??
        "";

      if (response.status === 401) {
        throw new ApiError(
          response.status,
          {
            code: "UNAUTHORIZED",
            message: "登录状态已失效，请重新登录",
            data: null,
          },
        );
      }

      if (contentType.includes("text/html")) {
        throw new Error(
          "接口返回了页面内容，请检查登录状态或本地代理是否正常",
        );
      }

      throw new Error(
        "后端返回的数据不是有效的 JSON",
      );
    }
  }

  if (!response.ok) {
    if (isApiErrorResponse(result)) {
      throw new ApiError(
        response.status,
        result,
      );
    }

    throw new Error(
      responseText ||
      `请求失败（${response.status}）`,
    );
  }

  return result as T;
}

export async function createCustomerServiceUser(
  request: CreateAgentRequest,
): Promise<CreateAgentResponse> {
  return apiRequest<CreateAgentResponse>(
    "/api/admin/customer-service-users",
    {
      method: "POST",
      body: JSON.stringify({
        name: request.name.trim(),
        phone: request.phone.trim(),
        email:
          request.email?.trim() ||
          null,
        initialPassword:
          request.initialPassword,
      }),
    },
  );
}

interface GetUsersParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  role?: UserRole | "ALL";
  status?: UserStatus | "ALL";
}

export async function getCustomerServiceUsers(
  params: GetUsersParams = {},
): Promise<UserListResponse> {
  const searchParams =
    new URLSearchParams();

  searchParams.set(
    "page",
    String(params.page ?? 1),
  );

  searchParams.set(
    "pageSize",
    String(params.pageSize ?? 10),
  );

  if (params.keyword?.trim()) {
    searchParams.set(
      "keyword",
      params.keyword.trim(),
    );
  }

  if (
    params.role &&
    params.role !== "ALL"
  ) {
    searchParams.set(
      "role",
      params.role,
    );
  }

  if (
    params.status &&
    params.status !== "ALL"
  ) {
    searchParams.set(
      "status",
      params.status,
    );
  }

  return apiRequest<UserListResponse>(
    `/api/admin/customer-service-users?${searchParams.toString()}`,
  );
}

export async function updateCustomerServiceUser(
  userId: number,
  request: UpdateAgentRequest,
): Promise<UpdateAgentResponse> {
  return apiRequest<UpdateAgentResponse>(
    `/api/admin/customer-service-users/${userId}`,
    {
      method: "PATCH",
      body: JSON.stringify({
        name: request.name.trim(),
        email:
          request.email?.trim() ||
          null,
      }),
    },
  );
}

export async function updateCustomerServiceUserStatus(
  userId: number,
  request: UpdateStatusRequest,
): Promise<UpdateStatusResponse> {
  return apiRequest<UpdateStatusResponse>(
    `/api/admin/customer-service-users/${userId}/status`,
    {
      method: "PATCH",
      body: JSON.stringify(request),
    },
  );
}

export async function resetCustomerServiceUserPassword(
  userId: number,
  request: ResetPasswordRequest,
): Promise<ResetPasswordResponse> {
  return apiRequest<ResetPasswordResponse>(
    `/api/admin/customer-service-users/${userId}/reset-password`,
    {
      method: "POST",
      body: JSON.stringify(request),
    },
  );
}

export async function getCustomerServiceUserOperationLogs(
  userId: number,
): Promise<OperationLogResponse> {
  return apiRequest<OperationLogResponse>(
    `/api/admin/customer-service-users/${userId}/operation-logs`,
  );
}

export interface CurrentUserResponse {
  code: string;
  message: string;
  data: {
    userId: number;
    username: string;
    status: UserStatus;
    roleType: "ADMIN" | "CUSTOMER_SERVICE";
  };
}

export async function getCurrentUser(): Promise<CurrentUserResponse> {
  return apiRequest<CurrentUserResponse>("/api/v1/auth/me");
}

export interface UserStatisticsResponse {
  code: string;
  message: string;
  data: {
    totalUsers: number;
    customerServiceCount: number;
    adminCount: number;
    disabledCount: number;
  };
}


export async function getUserStatistics(): Promise<UserStatisticsResponse> {

  return apiRequest<UserStatisticsResponse>(
    "/api/admin/customer-service-users/statistics",
  );

}
