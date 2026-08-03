import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios';
import type { ErrorResponse } from '../types/favorite';

const baseURL = import.meta.env.VITE_API_BASE_URL || '';

const instance: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

instance.interceptors.request.use(
  (config) => {
    config.withCredentials = true;
    return config;
  },
  (error) => Promise.reject(error)
);

instance.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.code === 'ECONNABORTED') {
      return Promise.reject({
        status: 0,
        message: '个人话术库加载超时，请稍后重试',
      });
    }
    if (error.response) {
      const data = error.response.data as ErrorResponse;
      return Promise.reject({
        status: error.response.status,
        message: data?.message || error.message || '请求失败',
        errors: data?.errors || {},
      });
    }
    return Promise.reject({
      status: 0,
      message: error.message || '网络错误',
    });
  }
);

export const request = <T = unknown>(config: AxiosRequestConfig): Promise<T> => {
  return instance.request(config).then((res) => res.data);
};

export default instance;
