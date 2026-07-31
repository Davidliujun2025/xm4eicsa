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

const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

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

  let result: unknown;

  try {
    result = await response.json();
  } catch {
    throw new Error(
      "后端返回的数据不是有效的 JSON",
    );
  }

  if (!response.ok) {
    throw new ApiError(
      response.status,
      result as ApiErrorResponse,
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
