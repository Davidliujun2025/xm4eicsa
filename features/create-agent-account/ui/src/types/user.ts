// 后端约定字符
export type UserRole = "CUSTOMER_SERVICE" | "SYSTEM_ADMIN";

export type UserStatus = "ENABLED" | "DISABLED";

export interface User {
  userId: number;
  name: string;
  phone: string;
  email: string | null;
  role: UserRole;
  roleName: "客服人员" | "系统管理员";
  status: UserStatus;
  statusName: "启用" | "禁用";
  createdAt: string;
}

export interface CreateAgentRequest {
  name: string;
  phone: string;
  email?: string | null;
  initialPassword: string;
}

export interface FieldErrors {
  name?: string;
  phone?: string;
  email?: string;
  initialPassword?: string;
}

export interface CreateAgentResponse {
  code: string;
  message: string;
  data: User | null;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  data?: {
    fieldErrors?: FieldErrors;
  } | null;
}