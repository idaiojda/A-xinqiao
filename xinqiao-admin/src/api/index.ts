import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from '@/types'

class ApiClient {
  private instance: AxiosInstance

  constructor() {
    this.instance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {
    // 请求拦截器
    this.instance.interceptors.request.use(
      (config) => {
        const authStore = useAuthStore()
        if (authStore.token) {
          config.headers.Authorization = `Bearer ${authStore.token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // 响应拦截器
    this.instance.interceptors.response.use(
      (response: AxiosResponse<ApiResponse<any> & any>) => {
        const { data } = response
        // 如果返回的是数组，直接认为成功
        if (Array.isArray(data)) {
          return response
        }
        // 检查是否是成功的响应
        const ok = data?.code === 200 || data?.code === 0 || data?.ok === true
        if (ok) {
          return response
        } else {
          ElMessage.error(data?.message || data?.error || '请求失败')
          return Promise.reject(new Error(data?.message || data?.error || '请求失败'))
        }
      },
      (error) => {
        if (error.response?.status === 401) {
          const authStore = useAuthStore()
          authStore.clearAuth()
          window.location.href = '/login'
        } else if (error.response?.status === 403) {
          ElMessage.error('没有权限访问该资源')
        } else if (error.response?.status >= 500) {
          ElMessage.error('服务器错误，请稍后重试')
        } else {
          ElMessage.error(error.message || '网络错误')
        }
        return Promise.reject(error)
      }
    )
  }

  // 封装请求方法
  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.get<ApiResponse<T> & any>(url, config)
    const d: any = response.data
    return (d && d.data !== undefined) ? d.data : d
  }

  async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.post<ApiResponse<T> & any>(url, data, config)
    const d: any = response.data
    return (d && d.data !== undefined) ? d.data : d
  }

  async put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.put<ApiResponse<T> & any>(url, data, config)
    const d: any = response.data
    return (d && d.data !== undefined) ? d.data : d
  }

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.delete<ApiResponse<T> & any>(url, config)
    const d: any = response.data
    return (d && d.data !== undefined) ? d.data : d
  }
}

export const api = new ApiClient()
export default api
