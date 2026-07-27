// src/utils/request.ts

import axios, { AxiosInstance, AxiosError, AxiosRequestConfig } from 'axios';
import { ErrorResponse } from '../types/favorite';

// 从环境变量读取API基础URL
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088';

// 创建axios实例
const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：自动添加 X-Staff-Id（可改为实际获取方式）
instance.interceptors.request.use(
  (config) => {
    // 这里可以模拟从localStorage或上下文获取staffId
    const staffId = localStorage.getItem('staffId') || 'demo-csr';
    config.headers['X-Staff-Id'] = staffId;
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：统一处理错误
instance.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response) {
      const data = error.response.data as ErrorResponse;
      const message = data?.message || error.message || '请求失败';
      // 可在此集成全局提示（如message.error），但为了避免耦合，仅抛出
      return Promise.reject({
        status: error.response.status,
        message,
        errors: data?.errors || {},
      });
    }
    return Promise.reject({
      status: 0,
      message: error.message || '网络错误',
    });
  }
);

// 封装请求方法，保持类型安全
export const request = <T = any>(config: AxiosRequestConfig): Promise<T> => {
  return instance.request(config).then((res) => res.data);
};

export default instance;