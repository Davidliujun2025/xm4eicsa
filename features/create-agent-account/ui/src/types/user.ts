export type UserRole =
  | "CUSTOMER_SERVICE"
  | "SYSTEM_ADMIN";

export type UserStatus =
  | "ENABLED"
  | "DISABLED";

export interface User {
  userId: number;
  name: string;
  phone: string;
  email: string | null;
  role: UserRole;
  roleName:
    | "客服人员"
    | "系统管理员";
  status: UserStatus;
  statusName:
    | "启用"
    | "禁用";
  createdAt: string;
  updatedAt?: string;
}

export interface CreateAgentRequest {
  name: string;
  phone: string;
  email?: string | null;
  initialPassword: string;
}

export interface UpdateAgentRequest {
  name: string;
  email?: string | null;
}

export interface UpdateStatusRequest {
  status: UserStatus;
}

export interface ResetPasswordRequest {
  initialPassword: string;
}

export interface FieldErrors {
  name?: string;
  phone?: string;
  email?: string;
  initialPassword?: string;
  status?: string;
}

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

export type CreateAgentResponse =
  ApiResponse<User | null>;

export interface UserListData {
  list: User[];
  total: number;
  page: number;
  pageSize: number;
}

export type UserListResponse =
  ApiResponse<UserListData>;

export type UpdateAgentResponse =
  ApiResponse<User>;

export type UpdateStatusResponse =
  ApiResponse<User>;

export type ResetPasswordResponse =
  ApiResponse<null>;

export interface OperationLog {
  logId: number;
  userId: number;
  action: string;
  detail: string;
  createdAt: string;
}

export interface OperationLogData {
  list: OperationLog[];
  total: number;
}

export type OperationLogResponse =
  ApiResponse<OperationLogData>;

export interface ApiErrorResponse {
  code: string;
  message: string;
  data?: {
    fieldErrors?: FieldErrors;
  } | null;
}