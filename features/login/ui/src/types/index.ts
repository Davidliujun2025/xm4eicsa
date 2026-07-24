// 登录表单数据
export interface LoginForm {
  account: string
  password: string
  remember: boolean
}

// 密码校验状态
export interface PwdChecks {
  lengthOk: boolean
  hasUpper: boolean
  hasLower: boolean
  hasNumber: boolean
  hasSpecial: boolean
  classCountOk: boolean
}

// 登录响应（根据实际后端调整）
export interface LoginResponse {
  code: string
  message: string
  data?: {
    user?: {
      id: number
      account: string
      displayName: string
    }
    redirectPath?: string
  }
}
