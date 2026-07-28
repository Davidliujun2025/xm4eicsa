import axios from 'axios'

export const apiClient = axios.create({
  baseURL: 'http://localhost:3000/api', // 请根据实际后端地址修改
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})