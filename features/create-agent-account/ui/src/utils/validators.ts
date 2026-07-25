import type {
    CreateAgentRequest,
    FieldErrors,
  } from "../types/user";
  
  export function validateName(name: string): string {
    const value = name.trim();
  
    if (!value) {
      return "请输入用户姓名";
    }
  
    if (value.length < 2 || value.length > 20) {
      return "用户姓名长度应为2–20个字符";
    }
  
    // 支持中文、英文、中英文组合和中间空格
    if (!/^[\u4e00-\u9fffA-Za-z]+(?:\s[\u4e00-\u9fffA-Za-z]+)*$/.test(value)) {
      return "用户姓名应支持中文、英文或中英文组合";
    }
  
    return "";
  }
  
  export function validatePhone(phone: string): string {
    if (!phone) {
      return "请输入手机号";
    }
  
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      return "请输入正确的中国大陆11位手机号";
    }
  
    return "";
  }
  
  export function validateEmail(email: string): string {
    if (!email) {
      return "";
    }
  
    const emailPattern =
      /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
  
    if (!emailPattern.test(email)) {
      return "请输入正确的邮箱地址";
    }
  
    return "";
  }
  
  
export function validatePassword(
    password: string,
    phone: string,
  ): string {
    if (!password) {
      return "请输入初始密码";
    }
  
    if (password.length < 12 || password.length > 20) {
      return "初始密码长度应为12～20位";
    }
  
    if (/\s/.test(password)) {
      return "初始密码不允许包含空格";
    }
  
    if (
      !/[A-Za-z]/.test(password) ||
      !/\d/.test(password)
    ) {
      return "初始密码必须同时包含英文字母和数字";
    }
  
    if (!/^[A-Za-z\d@#$%_!]+$/.test(password)) {
      return "初始密码仅支持字母、数字及 @、#、$、%、_、!";
    }
  
    if (password === phone) {
      return "初始密码不能与手机号完全一致";
    }
  
    return "";
  }
  
  export function validateCreateAgentForm(
    values: CreateAgentRequest,
  ): FieldErrors {
    const errors: FieldErrors = {};
  
    const nameError = validateName(values.name);
    const phoneError = validatePhone(values.phone);
    const emailError = validateEmail(values.email ?? "");
    const passwordError = validatePassword(
      values.initialPassword,
      values.phone,
    );
  
    if (nameError) errors.name = nameError;
    if (phoneError) errors.phone = phoneError;
    if (emailError) errors.email = emailError;
    if (passwordError) errors.initialPassword = passwordError;
  
    return errors;
  }