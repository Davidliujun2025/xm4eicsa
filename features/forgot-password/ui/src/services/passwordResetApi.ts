const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
const PASSWORD_RESET_BASE_PATH = '/api/auth/password-reset';

export interface VerifyAccountResponse {
  resetToken: string;
  username: string;
  maskedBoundPhone: string;
  expiresInSeconds: number;
}

export interface SendSmsCodeResponse {
  expiresInSeconds: number;
  cooldownSeconds: number;
}

export interface ResetPasswordResponse {
  reset: boolean;
  nextAction: string;
}

interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export class ApiRequestError extends Error {
  readonly code: string;

  constructor(message: string, code = 'REQUEST_FAILED') {
    super(message);
    this.name = 'ApiRequestError';
    this.code = code;
  }
}

async function post<T>(path: string, body: unknown): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${PASSWORD_RESET_BASE_PATH}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
  } catch {
    throw new ApiRequestError('无法连接后端服务，请确认 Spring Boot 已启动');
  }

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  if (!payload) {
    throw new ApiRequestError('后端响应格式不正确');
  }

  if (!response.ok || !payload.success) {
    throw new ApiRequestError(payload.message || '请求失败，请稍后重试', payload.code);
  }

  return payload.data;
}

export function verifyAccount(username: string) {
  return post<VerifyAccountResponse>('/account/verify', { username });
}

export function sendSmsCode(resetToken: string, phone: string) {
  return post<SendSmsCodeResponse>('/sms-code', { resetToken, phone });
}

export function resetPassword(
  resetToken: string,
  phone: string,
  smsCode: string,
  newPassword: string,
  confirmPassword: string,
) {
  return post<ResetPasswordResponse>('/confirm', {
    resetToken,
    phone,
    smsCode,
    newPassword,
    confirmPassword,
  });
}
