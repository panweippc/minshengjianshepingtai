import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/common'

/** Axios 实例：统一 baseURL、token 拦截、401/403 处理 */
const service: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 30000
})

// 请求拦截：统一携带 token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一处理业务码与鉴权失败
service.interceptors.response.use(
  (response) => {
    // 二进制流（文件预览/下载）不做业务码解析，直接返回原始响应
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data as Result
    if (res.code === 200) {
      return res as unknown as typeof response
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
    } else if (status === 403) {
      ElMessage.error('没有访问权限')
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

/** 大文件请求单独超时配置 */
export const uploadConfig: AxiosRequestConfig = { timeout: 600000 }

/** 统一请求方法：返回 Result<T> 类型，消除 axios 响应包裹类型 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service(config) as unknown as Promise<T>
}

export function get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config) as unknown as Promise<T>
}

export function post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as unknown as Promise<T>
}

export function put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as unknown as Promise<T>
}

export function del<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config) as unknown as Promise<T>
}

/** 获取二进制文件流（文件预览/下载，自动携带 token） */
export async function getBlob(url: string, config?: AxiosRequestConfig): Promise<Blob> {
  const response = await service.get(url, { ...config, responseType: 'blob' })
  const blob = response.data as Blob
  // 后端鉴权失败时返回的是 JSON，这里转换为错误提示
  if (blob.type === 'application/json') {
    const text = await blob.text()
    const json = JSON.parse(text) as Result
    ElMessage.error(json.msg || '文件获取失败')
    throw new Error(json.msg || '文件获取失败')
  }
  return blob
}

export default service
